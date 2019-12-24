package scales.verticles;

import scales.model.Config;
import scales.model.ConfigMessageCodec;
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

            vsuccess("Setup");
            mConfig = new Config(configAr.result());
            startPromise.complete();
        });

        retriever.listen(this::onConfigChange);
        retriever.configStream().exceptionHandler(e -> {
            verror("Config file not found");
            startPromise.fail("Config file not found");
        });
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
