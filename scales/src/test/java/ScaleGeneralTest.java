import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import scales.model.Config;
import scales.model.ConfigMessageCodec;
import scales.model.OriginID;
import scales.model.OriginIDCodec;
import scales.verticles.MainVerticle;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static scales.verticles.ConfigurationVerticle.EBA_CONFIG_FETCH;
import static scales.verticles.ConfigurationVerticle.EBA_CONFIG_UPDATE;


@ExtendWith(VertxExtension.class)
abstract class ScaleGeneralTest {

    // config data

    private String mKafkaHost;
    private String mKafkaPort;
    private String mPutRequest;
    private String mDeleteRequest;
    private String mUserpicsTopic;
    private String mPhotosTopic;
    private String mSagasTopic;

    // test-specific data

    private ArrayList<String> mSagasResponses;
    private Integer mPutRequestsNum = 0;
    private Integer mDelRequestsNum = 0;
    private HashMap<String, Integer> mImagesRequests = new HashMap<>();

    private KafkaProducer<String, String> mProducer;
    private KafkaConsumer<String, String> mSagasConsumer;

    private Vertx mVertx;

    @BeforeEach
    public void setUp() throws IOException {
        setupVertx().setHandler(ar -> {
            if (ar.failed()) {
                throw new RuntimeException("Can't setup vertx");
            }
            Promise<Void> start = Promise.promise();

            registerCodecs();
            setupConfigListener();
            setupConfig(start);

            start.future().setHandler(setup -> {
                if (setup.failed()) {
                    throw new RuntimeException(setup.cause().getMessage());
                }
                actualTests().setHandler(tests -> {
                    checkForRequestResults();
                });
            });
        });
    }

    abstract Future<Void> actualTests();


    @Test
    /** Check:
     *  1. If number of put and del responses is equal to that of requests
     *  2. If all responses is "ok"
     *  3. If number of image name occurrences in responses is equal to that of requests
     */
    private void checkForRequestResults() {
        int putReqNum = 0, delReqNum = 0;
        int commandStart = "photo-scale:".length();
        for (String req : mSagasResponses) {
            int statusStart;
            if (req.substring(commandStart).startsWith(mDeleteRequest)) {
                delReqNum++;
                statusStart = commandStart + mDeleteRequest.length();
            } else {
                putReqNum++;
                statusStart = commandStart + mPutRequest.length();
            }

            assertTrue(req.substring(statusStart).startsWith("ok:"));
            String imageName = req.substring(statusStart + "ok:".length());

            mImagesRequests.compute(imageName, (k, v) -> {
                        assertNotNull(v);
                        return v - 1;
                    }
            );
        }

        assertEquals(putReqNum, (int) mPutRequestsNum);
        assertEquals(delReqNum, (int) mDelRequestsNum);

        for (Map.Entry<String, Integer> stringIntegerEntry : mImagesRequests.entrySet()) {
            Integer occurrNum = stringIntegerEntry.getValue();
            assertEquals(occurrNum, 0);
        }
    }

    private void toPhotosTopic(String request) {
        toTopic(request, mPhotosTopic);
    }

    private void toUserpicsTopic(String request) {
        toTopic(request, mUserpicsTopic);
    }

    private void toTopic(String request, String topic) {
        mProducer.write(
                KafkaProducerRecord.create(topic, request)
        );

        String name;
        if (request.startsWith(mDeleteRequest)) {
            mDelRequestsNum++;
            name = request.substring(mDeleteRequest.length());
        } else {
            mPutRequestsNum++;
            name = request.substring(mPutRequest.length());
        }
        mImagesRequests.compute(name, (k, v) -> (v == null) ? 1 : v + 1);
    }

    private Future<Void> setupVertx() {
        Promise<Void> start = Promise.promise();
        mVertx = Vertx.vertx();
        mVertx.deployVerticle(MainVerticle.class.getName(), ar -> {
            if (ar.failed()) {
                start.fail("Main verticle deployment");
            } else {
                start.complete();
            }
        });

        return start.future();
    }

    private void setupFromConfig(@Nonnull Config config, Promise<Void> start) {
        mKafkaHost = config.getKafkaHost();
        mKafkaPort = config.getKafkaPort();
        mUserpicsTopic = config.getUserpicsTopic();
        mPhotosTopic = config.getPhotosTopic();
        mDeleteRequest = config.getDeleteRequest();
        mPutRequest = config.getScaleRequest();
        mSagasTopic = config.getSagasTopic();

        if (mSagasConsumer != null) mSagasConsumer.unsubscribe();
        setupSagasConsumer(start);
        setupKafkaProducer();
    }


    private void setupSagasConsumer(Promise<Void> start) {
        Map<String, String> kafkaConfig = new HashMap<>();
        kafkaConfig.put("bootstrap.servers", String.join(":", mKafkaHost, mKafkaPort));
        kafkaConfig.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaConfig.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaConfig.put("group.id", "my_group");
        kafkaConfig.put("auto.offset.reset", "earliest");
        kafkaConfig.put("enable.auto.commit", "true");

        mSagasConsumer = KafkaConsumer.create(mVertx, kafkaConfig);

        mSagasConsumer.handler(record -> {
            mSagasResponses.add(record.value());
        });

        mSagasConsumer.subscribe(mSagasTopic, ar -> {
            if (ar.succeeded()) {
                start.complete();
            } else {
                start.fail("Failed subscribing " + mSagasTopic);
            }
        });
    }


    private void setupKafkaProducer() {
        Map<String, String> kafkaConfig = new HashMap<>();
        kafkaConfig.put("bootstrap.servers", String.join(":", mKafkaHost, mKafkaPort));
        kafkaConfig.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaConfig.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaConfig.put("group.id", "my_group");
        kafkaConfig.put("auto.offset.reset", "earliest");
        kafkaConfig.put("enable.auto.commit", "true");


        mProducer = KafkaProducer.create(mVertx, kafkaConfig);
    }


    /**
     * Set our channels of communication using Config and Profile classes
     * and codecs for them
     */
    private void registerCodecs() {
        try {
            mVertx.eventBus().registerDefaultCodec(Config.class, new ConfigMessageCodec());
        } catch (IllegalStateException ignored) {
        }
        try {
            mVertx.eventBus().registerDefaultCodec(OriginID.class, new OriginIDCodec());
        } catch (IllegalStateException ignored) {
        }
    }


    /**
     * Listen on configuration changes and update sizes accordingly
     */
    private void setupConfigListener() {
        mVertx.eventBus().<Config>consumer(EBA_CONFIG_UPDATE, configAr -> {
            setupFromConfig(configAr.body(), Promise.promise());
        });
    }

    private void setupConfig(Promise<Void> startPromise) {
        Promise<Config> promise = Promise.promise();
        promise.future().setHandler(configAr -> {
            if (configAr.failed()) {
                startPromise.fail("Can't fetch config");
            }
        });

        fetchConfig(promise, startPromise);
    }


    private void fetchConfig(Promise<Config> promise, Promise<Void> startPromise) {
        mVertx.eventBus().<Config>request(EBA_CONFIG_FETCH, new JsonObject(), configAr -> {
            if (configAr.failed()) {
                promise.fail(configAr.cause());
            } else {
                setupFromConfig(configAr.result().body(), startPromise);
            }
        });
    }
}