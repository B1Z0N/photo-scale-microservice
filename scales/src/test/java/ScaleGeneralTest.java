import io.vertx.config.ConfigRetriever;
import io.vertx.core.*;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import scales.model.Config;

import javax.annotation.Nonnull;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(VertxExtension.class)
abstract class ScaleGeneralTest {

    // config data

    private String mKafkaHost;
    private String mKafkaPort;
    private String mPutRequest;
    private String mDelRequest;
    private String mUserpicsTopic;
    private String mPhotosTopic;
    private String mSagasTopic;

    // test-specific data

    private ArrayList<String> mSagasResponses = new ArrayList<>();
    private Integer mPutRequestsNum = 0;
    private Integer mDelRequestsNum = 0;
    private HashMap<String, Integer> mImagesRequests = new HashMap<>();

    // number of requests to check if we could start making assertions
    private Integer mRequestsNum = 0;
    // and promise that signals finish of computing
    private Promise<Void> mFinishPromise = Promise.promise();

    private KafkaProducer<String, String> mProducer;
    private KafkaConsumer<String, String> mSagasConsumer;

    private Vertx mVertx;

    @BeforeAll
    void setUp(Vertx vertx, VertxTestContext testContext) {
        deployVertx().setHandler(deployAr -> {
            Promise<Void> start = Promise.promise();
            start.future().setHandler(ar -> {
                if (ar.failed()) {
                    testContext.failNow(ar.cause());
                } else {
                    actualTests();
                }
            });

            setupConfig(start);
        });

        // start assertions after all of the code
        mFinishPromise.future().setHandler(finishAr -> {
            mSagasConsumer.commit();
            assertTrue(finishAr.succeeded());
            testContext.completeNow();
        });
    }


    // run some operations in descendant classes
    // use putPhoto, delPhoto, putUserpic, delUserpic inside
    abstract void actualTests();


    /* Check:
       1. If number of put and del responses is equal to that of requests
       2. If all responses is "ok"
       3. If number of image name occurrences in responses is equal to that of requests
     */
    @Test
    void checkForRequestResults(Vertx vertx, VertxTestContext testContext) {
        int putReqNum = 0, delReqNum = 0;
        int commandStart = "photo-scale:".length();
        for (String response : mSagasResponses) {
            int statusStart;
            if (response.substring(commandStart).startsWith(mDelRequest)) {
                delReqNum++;
                statusStart = commandStart + mDelRequest.length();
            } else {
                putReqNum++;
                statusStart = commandStart + mPutRequest.length();
            }

            // status should be okay
            // if not - relax, it is not code error
            // it's just report to sagas
            assumeTrue(response.substring(statusStart).startsWith("ok:"));
            String imageName = response.substring(statusStart + "ok:".length());

            mImagesRequests.compute(imageName, (k, v) -> {
                        // this image name must exist in requests
                        assertNotNull(v);
                        return v - 1;
                    }
            );
        }

        // check if number of delRequests and put requests are the same
        // of that on request and response side
        assertEquals(putReqNum, (int) mPutRequestsNum);
        assertEquals(delReqNum, (int) mDelRequestsNum);

        for (Map.Entry<String, Integer> stringIntegerEntry : mImagesRequests.entrySet()) {
            Integer occurrNum = stringIntegerEntry.getValue();
            // number of responses to current image name
            // negated the number of requests
            // should be zero
            assertEquals(occurrNum, 0);
        }

        testContext.completeNow();
    }

    // kafka communication methods

    void putUserpic(String imgName) {
        toTopic(mPutRequest + imgName, mUserpicsTopic);
    }

    void delUserpic(String imgName) {
        toTopic(mDelRequest + imgName, mUserpicsTopic);
    }

    void putPhoto(String imgName) {
        toTopic(mPutRequest + imgName, mPhotosTopic);
    }

    void delPhoto(String imgName) {
        toTopic(mDelRequest + imgName, mPhotosTopic);
    }


    private void toTopic(String request, String topic) {
        mProducer.write(
                KafkaProducerRecord.create(topic, request)
        );

        String name;
        if (request.startsWith(mDelRequest)) {
            mDelRequestsNum++;
            name = request.substring(mDelRequest.length());
        } else {
            mPutRequestsNum++;
            name = request.substring(mPutRequest.length());
        }
        mImagesRequests.compute(name, (k, v) -> (v == null) ? 1 : v + 1);
        mRequestsNum++;
    }


    // deployment and setup methods


    private void setupSagasConsumer(Promise<Void> start) {
        Map<String, String> kafkaConfig = new HashMap<>();
        kafkaConfig.put("bootstrap.servers", String.join(":", mKafkaHost, mKafkaPort));
        kafkaConfig.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaConfig.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaConfig.put("group.id", "my_group");
        kafkaConfig.put("auto.offset.reset", "latest");
        kafkaConfig.put("enable.auto.commit", "true");

        mSagasConsumer = KafkaConsumer.create(mVertx, kafkaConfig);

        mSagasConsumer.handler(record -> {
            mSagasResponses.add(record.value());
            // if all sent requests are captured responses, then start assertions
            if (--mRequestsNum == 0) {
                mFinishPromise.complete();
            }
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

        mProducer = KafkaProducer.create(mVertx, kafkaConfig);
    }


    private Future<Void> deployVertx() {
        Promise<Void> deploy = Promise.promise();
        mVertx = Vertx.vertx();
        deploy.complete();
        return deploy.future();
    }


    // config-relative methods


    private void setupConfig(Promise<Void> startPromise) {
        ConfigRetriever retriever = ConfigRetriever.create(mVertx);
        retriever.getConfig(configAr -> {
            if (configAr.failed()) {
                startPromise.fail(configAr.cause());
            } else {
                setupFromConfig(new Config(configAr.result()), startPromise);
            }
        });

        retriever.configStream().exceptionHandler(e -> startPromise.fail(e.getMessage()));
    }


    private void setupFromConfig(@Nonnull Config config, Promise<Void> start) {
        mKafkaHost = config.getKafkaHost();
        mKafkaPort = config.getKafkaPort();
        mUserpicsTopic = config.getUserpicsTopic();
        mPhotosTopic = config.getPhotosTopic();
        mDelRequest = config.getDeleteRequest();
        mPutRequest = config.getScaleRequest();
        mSagasTopic = config.getSagasTopic();

        if (mSagasConsumer != null) mSagasConsumer.unsubscribe();
        setupKafkaProducer();
        setupSagasConsumer(start);
    }
}
