package profiles.verticles;

import com.sun.xml.bind.v2.model.core.ID;
import profiles.model.Config;
import profiles.model.ConfigMessageCodec;
import profiles.model.OriginID;
import profiles.model.OriginID.photoType;
import profiles.model.OriginIDCodec;
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
import static profiles.verticles.ScaleVerticle.EBA_DELETE_ORIGIN;
import static profiles.verticles.ScaleVerticle.EBA_SCALE_ORIGIN;

public class ApiVerticle extends MicroserviceVerticle {

    // Overrides

    @Override
    public void start() throws InterruptedException {
        createServiceDiscovery();
        registerCodecs();
        photoScale("website.jpg", photoType.PHOTO);
//        photoDelete("website.jpg", photoType.PHOTO);
    }

    // Private

    private void photoScale(@Nonnull String ID, photoType type) {
        vertx.eventBus().<OriginID>request(EBA_SCALE_ORIGIN, new OriginID(ID, type), ar -> {
            if (ar.succeeded()) {
                // send "OK" to sagas
                System.out.println("SCALE OK! ID: " + ID.toString());
            } else {
                // send "ERR" to sagas
                System.out.println("SCALE ERR! ID: " + ID.toString() + " | " + ar.cause());
            }
        });
    }

    private void photoDelete(@Nonnull String ID, photoType type) {
        vertx.eventBus().<OriginID>request(EBA_DELETE_ORIGIN, new OriginID(ID, type), ar -> {
            if (ar.succeeded()) {
                // send "OK" to sagas
                System.out.println("DELETE OK! ID: " + ID.toString());
            } else {
                // send "ERR" to sagas
                System.out.println("DELETE ERR! ID: " + ID.toString() + " | " + ar.cause());
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
        } catch (IllegalStateException ignored) {
        }
        try {
            vertx.eventBus().registerDefaultCodec(OriginID.class, new OriginIDCodec());
        } catch (IllegalStateException ignored) {
        }
    }
}
