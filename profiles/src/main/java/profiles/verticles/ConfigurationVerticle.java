package profiles.verticles;

import profiles.model.Config;
import profiles.model.ConfigMessageCodec;
import vertx.common.MicroserviceVerticle;
import io.vertx.config.ConfigChange;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

/**
 * Verticle for configuration maintenance update/retrieval
 */
public class ConfigurationVerticle extends MicroserviceVerticle {

    // Constants

    static final String EBA_CONFIG_FETCH = "configuration:fetch";
    static final String EBA_CONFIG_UPDATE = "configuration:update";

    // Variables

    private Config mConfig;

    // Overrides

    @Override
    public void start(Promise<Void> startPromise) {
        createServiceDiscovery();
        registerCodecs();
        setupRetriever(startPromise);
        setupListener();
    }

    // Private

    private void registerCodecs() {
        try {
            vertx.eventBus().registerDefaultCodec(Config.class, new ConfigMessageCodec());
        } catch (IllegalStateException ignored) {
        }
    }

    private void setupRetriever(Promise<Void> startPromise) {
        ConfigRetriever retriever = ConfigRetriever.create(vertx);
        retriever.getConfig(configAr -> {
            if (configAr.failed()) {
                verror("Setup");
                startPromise.fail(configAr.cause());
                return;
            }

            mConfig = new Config(configAr.result());

            publishMessageSource(EBA_CONFIG_UPDATE, EBA_CONFIG_UPDATE, publishAr -> {
                if (publishAr.failed()) {
                    verror("Setup");
                    startPromise.fail(publishAr.cause());
                } else {
                    vertx.eventBus().publish(EBA_CONFIG_UPDATE, mConfig);

                    startPromise.complete();
                    vsuccess("Setup");
                }
            });
        });

        retriever.listen(this::onConfigChange);
        retriever.configStream().exceptionHandler(e -> verror("Condig file not found"));
    }

    private void onConfigChange(ConfigChange change) {
        JsonObject configJson = change.getNewConfiguration();
        mConfig = new Config(configJson);

        vertx.eventBus().publish(EBA_CONFIG_UPDATE, mConfig);
    }

    private void setupListener() {
        vertx.eventBus().<Config>consumer(EBA_CONFIG_FETCH, handler -> handler.reply(mConfig));
    }
}
