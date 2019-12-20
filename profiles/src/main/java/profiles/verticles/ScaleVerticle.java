package profiles.verticles;

import profiles.model.Config;
import profiles.model.ConfigMessageCodec;
import profiles.model.ImageScalesURLs;
import profiles.model.ImageScalesURLsCodec;
import profiles.model.OriginURL;
import profiles.model.OriginURLCodec;
import profiles.utility.ImageResize

import vertx.common.MicroserviceVerticle;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;

import static profiles.verticles.ConfigurationVerticle.EBA_CONFIG_FETCH;
import static profiles.verticles.ConfigurationVerticle.EBA_CONFIG_UPDATE;

/**
 * Verticle that replies to Profile requests
 */
public class ScaleVerticle extends MicroserviceVerticle {

    // Constants

    public static final String EBA_SCALE_ORIGIN = "put:origin";
//    public static final String EBA_GET_SCALE = "get:scale";
//    public static final String EBA_DELETE_ORIGIN = "delete:origin";

    private HashMap<String, Integer> mSizes;

    // Overrides

    @Override
    public void start(Promise<Void> startPromise) {
        createServiceDiscovery();
        registerCodecs();
        setupConfigListener();
        setupConfig();
        setupListeners();
    }

    // Private

    private void registerCodecs() {
        try {
            vertx.eventBus().registerDefaultCodec(OriginURL.class, new OriginURLCodec());
            vertx.eventBus().registerDefaultCodec(ImageScalesURLs.class, new ImageScalesURLsCodec());
            vertx.eventBus().registerDefaultCodec(Config.class, new ConfigMessageCodec());
        } catch (IllegalStateException ignored) {
        }
    }

    private void setupListeners() {
        vertx.eventBus().<JsonObject>consumer(EBA_SCALE_ORIGIN, handler -> {
            // resize and put to s3, insert into db
        });
    }

    /**
     * Listen on configuration changes and update sizes accordingly
     */
    private void setupConfigListener() {
        vertx.eventBus().<Config>consumer(EBA_CONFIG_UPDATE, configAr -> {
            mSizes = configAr.body().getSizes();
            System.out.println("New sizes came up: " + mSizes);
        });
    }

    private void setupConfig() {
        Promise<Config> promise = Promise.promise();
        promise.future().setHandler(configAr -> {
            if (configAr.failed()) {
                System.out.println("Scales can't be fetched correctly: " + configAr.cause().getMessage());
            } else {
                System.out.println("New sizes came up: " + configAr.result().getSizes().toString());
            }
        });
        fetchConfig(promise);
    }

    /**
     * Get sizes from eventbus and pass it to promise
     */
    private void fetchConfig(Promise<Config> promise) {
        vertx.eventBus().<Config>request(EBA_CONFIG_FETCH, new JsonObject(), configAr -> {
            if (configAr.failed()) {
                promise.fail(configAr.cause());
                return;
            }

            Config config = configAr.result().body();
            mSizes = config.getSizes();
        });
    }
}
