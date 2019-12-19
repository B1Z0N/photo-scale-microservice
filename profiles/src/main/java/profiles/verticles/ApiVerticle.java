package profiles.verticles;

import profiles.model.Config;
import profiles.model.ConfigMessageCodec;
import profiles.model.OriginURL;
import profiles.model.OriginURLCodec;
import profiles.model.ImageScalesURLs;
import profiles.model.ImageScalesURLsCodec;

import vertx.common.MicroserviceVerticle;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;

import javax.annotation.Nonnull;
import java.io.File;

import static profiles.verticles.ConfigurationVerticle.EBA_CONFIG_FETCH;
import static profiles.verticles.ConfigurationVerticle.EBA_CONFIG_UPDATE;
import static profiles.verticles.ScaleVerticle.EBA_SCALE_ORIGIN;

public class ApiVerticle extends MicroserviceVerticle {

  // Variables

  private VertxServer mServer;

  // Overrides

  @Override
  public void start() {
    createServiceDiscovery();
    registerCodecs();
    setupConfigListener();
    setupConfig();
    sendPhotoRequest(1, "https://i.pinimg.com/originals/ec/b9/44/ecb94420f2844e9213969076e3aa2dcb.jpg");
  }

  // Private

  private void sendPhotoRequest(@Nonnull Integer ID, @Nonnull String URL) {
    vertx.eventBus().<OriginURL>request(EBA_SCALE_ORIGIN, new OriginURL(ID, URL), ar -> {
      if (ar.succeeded()) {
        // send "OK" to sagas
        System.out.println("OK! ID: " + ID.toString() + " | " + ar.cause());
      } else {
        // send "ERR" to sagas
        System.out.println("ERR! ID: " + ID.toString());
      }
    });
  }


  /**
   * Set our channels of communication using Config and Profile classes
   * and codecs for them
   */
  private void registerCodecs() {
    try {
      vertx.eventBus().registerDefaultCodec(Config.class, new ConfigMessageCodec());
      vertx.eventBus().registerDefaultCodec(OriginURL.class, new OriginURLCodec());
    } catch (IllegalStateException ignored) {}
  }

  /** Listen on configuration changes and update API server accordingly */
  private void setupConfigListener() {
    vertx.eventBus().<Config>consumer(EBA_CONFIG_UPDATE, configAr -> {
      setupServer(configAr.body()).future().setHandler(serverAr -> {
        if (serverAr.failed()) {
          System.out.println("API server restart failed: " + serverAr.cause().getMessage());
        } else {
          System.out.println("API server restarted on " + configAr.body().getEndpointHost() + ":" + configAr.body().getEndpointPort());
        }
      });
    });
  }

  private void setupConfig() {
    Promise<Config> promise = Promise.promise();
    promise.future().setHandler(configAr -> {
      if (configAr.failed()) {
        System.out.println("API server start failed: " + configAr.cause().getMessage());
      } else {
        System.out.println("API server started on " + configAr.result().getEndpointHost() + ":" + configAr.result().getEndpointPort());
      }
    });
    fetchConfig(promise);
  }

  /** Get config from eventbus and pass it to promise */
  private void fetchConfig(Promise<Config> promise) {
    vertx.eventBus().<Config>request(EBA_CONFIG_FETCH, new JsonObject(), configAr -> {
      if (configAr.failed()) {
        promise.fail(configAr.cause());
        return;
      }

      Config config = configAr.result().body();
      setupServer(config).future().setHandler(serverAr -> {
        if (serverAr.failed()) {
          promise.fail(serverAr.cause());
        } else {
          promise.complete(config);
        }
      });
    });
  }

  private Promise<Void> setupServer(@Nonnull Config config) {
    Promise<Void> promise = Promise.promise();

    if (mServer != null) mServer.shutdown();

    mServer = VertxServerBuilder.forAddress(vertx, config.getEndpointHost(), Integer.parseInt(config.getEndpointPort()))
      .useTransportSecurity(certChainFile(config), privateKeyFile(config))
      /** Important, adding service for retrieval of Profile data via gRPC*/
      .addService(ProtoReflectionService.newInstance())
      .build()
      .start(ar -> {
        if (ar.failed()) {
          promise.fail(ar.cause());
        } else {
          publishHttpEndpoint(
            "API endpoint",
            config.getEndpointHost(),
            Integer.parseInt(config.getEndpointPort()),
            publishAr -> {});

          promise.complete();
        }
      });

    return promise;
  }

  private File certChainFile(@Nonnull Config config) {
    return new File(config.getTlsCertChain());
  }

  private File privateKeyFile(@Nonnull Config config) {
    return new File(config.getTlsPrivKey());
  }
}
