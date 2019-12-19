package profiles.verticles;

import profiles.model.Config;
import profiles.model.ConfigMessageCodec;
import profiles.model.ImageScalesURLs;
import profiles.model.ImageScalesURLsCodec;
import profiles.model.OriginURL;
import profiles.model.OriginURLCodec;

import vertx.common.MicroserviceVerticle;

import io.vertx.config.ConfigChange;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;

/** Verticle that replies to Profile requests */
public class ScaleVerticle extends MicroserviceVerticle {

    // Constants

    public static final String EBA_SCALE_ORIGIN = "scale:origin";
    public static final String EBA_UPDATE_SIZES = "update:sizes";

    // Overrides

    @Override
    public void start(Promise<Void> startPromise) {
        createServiceDiscovery();
        registerCodecs();
    }

    // Private

    private void registerCodecs() {
        try {
            vertx.eventBus().registerDefaultCodec(OriginURL.class, new OriginURLCodec());
            vertx.eventBus().registerDefaultCodec(ImageScalesURLs.class, new ImageScalesURLsCodec());
        } catch (IllegalStateException ignored) {}
    }

    private void setupScaleListener() {
        vertx.eventBus().<JsonObject>consumer(EBA_SCALE_ORIGIN, handler -> {

        });
    }

    private void setupSizesListener() {
        vertx.eventBus().<JsonObject>consumer(EBA_UPDATE_SIZES, handler -> {

        });
    }
}