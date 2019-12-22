package profiles.verticles;

import com.sun.xml.bind.v2.model.core.ID;
import io.vertx.core.Vertx;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
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
import java.util.HashMap;
import java.util.Map;

import static profiles.verticles.ConfigurationVerticle.EBA_CONFIG_FETCH;
import static profiles.verticles.ConfigurationVerticle.EBA_CONFIG_UPDATE;
import static profiles.verticles.ScaleVerticle.EBA_DELETE_ORIGIN;
import static profiles.verticles.ScaleVerticle.EBA_SCALE_ORIGIN;

public class ApiVerticle extends MicroserviceVerticle {

    // Overrides

    KafkaConsumer<String, String> mConsumer;

    private String mKafkaHost = "localhost";
    private String mKafkaPort = "9092";
    private String mUserpicsTopic;
    private String mPhotosTopic;


    @Override
    public void start(Promise<Void> startPromise) throws InterruptedException {
        createServiceDiscovery();
        registerCodecs();
        setupConfigListener();
        setupConfig(startPromise);
    }

    // Private
    private void setupFromConfig(@Nonnull Config config) {
        mKafkaHost = config.getKafkaHost();
        mKafkaPort = config.getKafkaPort();
        mUserpicsTopic = config.getUserpicsTopic();
        mPhotosTopic = config.getPhotosTopic();
        setupKafkaConsumers();
    }

    public void setupKafkaConsumers() {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", String.join(":", mKafkaHost, mKafkaPort));
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("acks", "1");

        mConsumer = KafkaConsumer.create(Vertx.vertx(), config);
        mConsumer.handler(record -> {
            System.out.println("Record: " + record.topic() + " | " + record.value());

            photoType type = record.topic().equals(mUserpicsTopic) ? photoType.USERPIC : photoType.PHOTO;

            if (record.value().startsWith("del:")) photoScale(record.value().substring(4), type);
            else if (record.value().startsWith("put:")) photoDelete(record.value().substring(4), type);
        });

        mConsumer.subscribe(mUserpicsTopic, ar -> {
            if (ar.succeeded()) {
                System.out.println(String.format("Subscribed to %s successfully", mUserpicsTopic));
            } else {
                System.out.println(
                        String.format("Could not subscribe to %s: ", mUserpicsTopic) + ar.cause().getMessage()
                );
            }
        });

        mConsumer.subscribe(mPhotosTopic, ar -> {
            if (ar.succeeded()) {
                System.out.println(String.format("Subscribed to %s successfully", mPhotosTopic));
            } else {
                System.out.println(
                        String.format("Could not subscribe to %s: ", mPhotosTopic) + ar.cause().getMessage()
                );
            }
        });
    }

    private void photoScale(@Nonnull String ID, photoType type) {
        vertx.eventBus().<OriginID>request(EBA_SCALE_ORIGIN, new OriginID(ID, type), ar -> {
            if (ar.failed()) {
                // send "ERR" to sagas
                System.out.println("SCALE ERR! ID: " + ID.toString() + " | " + ar.cause());
                return;
            }

            // send "OK" to sagas
            System.out.println("SCALE OK! ID: " + ID.toString());
        });
    }

    private void photoDelete(@Nonnull String ID, photoType type) {
        vertx.eventBus().<OriginID>request(EBA_DELETE_ORIGIN, new OriginID(ID, type), ar -> {
            if (ar.failed()) {
                // send "ERR" to sagas
                System.out.println("DELETE ERR! ID: " + ID.toString() + " | " + ar.cause());
                return;
            }

            // send "OK" to sagas
            System.out.println("DELETE OK! ID: " + ID.toString());
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


    /**
     * Listen on configuration changes and update sizes accordingly
     */
    private void setupConfigListener() {
        vertx.eventBus().<Config>consumer(EBA_CONFIG_UPDATE, configAr -> {
            setupFromConfig(configAr.body());
            System.out.println("New kafka setup came up: " + mSizes);
        });
    }

    private void setupConfig(Promise<Void> startPromise) {
        Promise<Config> promise = Promise.promise();
        promise.future().setHandler(configAr -> {
            if (configAr.failed()) {
                System.out.println("1can't be fetched correctly: " + configAr.cause().getMessage());
            } else {
                System.out.println("New sizes came up: " + configAr.result().getSizes().toString());
            }
        });
        fetchConfig(promise, startPromise);
    }

    /**
     * Get sizes from eventbus and pass it to promise
     */
    private void fetchConfig(Promise<Config> promise, Promise<Void> startPromise) {
        vertx.eventBus().<Config>request(EBA_CONFIG_FETCH, new JsonObject(), configAr -> {
            if (configAr.failed()) {
                promise.fail(configAr.cause());
                startPromise.fail(configAr.cause());
                return;
            }

            setupFromConfig(configAr.result().body());
            startPromise.complete();
        });
    }
}
