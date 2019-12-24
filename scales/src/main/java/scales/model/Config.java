package scales.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Data class: representation of conf/config.json in plain java class
 */
public class Config {

    // Constants

    private static final String SIZES = "sizes";
    private static final String NAME = "name";
    private static final String WIDTH = "width";

    private static final String AWS = "aws";
    private static final String PHOTOS_BUCKET = "photosBucket";
    private static final String USERPICS_BUCKET = "userpicsBucket";
    private static final String REGION = "region";
    private static final String EXTENSION = "extension";

    private static final String KAFKA = "kafka";
    private static final String KAFKA_HOST = "host";
    private static final String KAFKA_PORT = "port";
    private static final String PHOTOS_TOPIC = "photosTopic";
    private static final String USERPICS_TOPIC = "userpicsTopic";
    private static final String SAGAS_TOPIC = "sagasTopic";
    private static final String DELETE_REQUEST = "deleteRequest";
    private static final String SCALE_REQUEST = "scaleRequest";

    // Variables

    private final JsonObject mConfigObject;

    private final HashMap<String, Integer> mSizes;
    private final String mRegion;
    private final String mPhotosBucket;
    private final String mUserpicsBucket;

    private final String mExtension;

    private final String mKafkaHost;
    private final String mKafkaPort;
    private final String mPhotosTopic;
    private final String mUserpicsTopic;
    private final String mSagasTopic;

    private final String mDeleteRequest;
    private final String mScaleRequest;

    // Constructors

    public Config(@Nonnull JsonObject config) {
        mConfigObject = config;

        JsonArray sizes = config.getJsonArray(SIZES);
        mSizes = jsonSizesArrayToMap(sizes);

        JsonObject aws = config.getJsonObject(AWS);
        mRegion = aws.getString(REGION);
        mUserpicsBucket = aws.getString(USERPICS_BUCKET);
        mPhotosBucket = aws.getString(PHOTOS_BUCKET);

        mExtension = config.getString(EXTENSION);

        JsonObject kafka = config.getJsonObject(KAFKA);
        mKafkaHost = kafka.getString(KAFKA_HOST);
        mKafkaPort = kafka.getString(KAFKA_PORT);
        mUserpicsTopic = kafka.getString(USERPICS_TOPIC);
        mPhotosTopic = kafka.getString(PHOTOS_TOPIC);
        mSagasTopic = kafka.getString(SAGAS_TOPIC);
        mDeleteRequest = kafka.getString(DELETE_REQUEST);
        mScaleRequest = kafka.getString(SCALE_REQUEST);
    }

    // Public

    JsonObject toJson() {
        JsonArray sizes = mapSizesToJsonArray(mSizes);

        JsonObject aws = new JsonObject()
                .put(PHOTOS_BUCKET, mPhotosBucket)
                .put(USERPICS_BUCKET, mUserpicsBucket)
                .put(REGION, mRegion);

        JsonObject kafka = new JsonObject()
                .put(KAFKA_HOST, mKafkaHost)
                .put(KAFKA_PORT, mKafkaPort)
                .put(PHOTOS_TOPIC, mPhotosTopic)
                .put(USERPICS_TOPIC, mUserpicsTopic)
                .put(SAGAS_TOPIC, mSagasTopic)
                .put(DELETE_REQUEST, mDeleteRequest)
                .put(SCALE_REQUEST, mScaleRequest);

        return new JsonObject()
                .put(SIZES, sizes)
                .put(EXTENSION, mExtension)
                .put(AWS, aws)
                .put(KAFKA, kafka);
    }

    // Accessors

    JsonObject getConfigObject() {
        return mConfigObject;
    }

    public HashMap<String, Integer> getSizes() {
        return mSizes;
    }

    public String getRegion() {
        return mRegion;
    }

    public String getPhotosBucket() {
        return mPhotosBucket;
    }

    public String getUserpicsBucket() {
        return mUserpicsBucket;
    }

    public String getExtension() {
        return mExtension;
    }

    public String getKafkaHost() {
        return mKafkaHost;
    }

    public String getKafkaPort() {
        return mKafkaPort;
    }

    public String getUserpicsTopic() {
        return mUserpicsTopic;
    }

    public String getPhotosTopic() {
        return mPhotosTopic;
    }

    public String getDeleteRequest() {
        return mDeleteRequest;
    }

    public String getScaleRequest() {
        return mScaleRequest;
    }

    public String getSagasTopic() { return mSagasTopic; }

    // Utils

    private HashMap<String, Integer> jsonSizesArrayToMap(@Nonnull JsonArray jarr) {
        HashMap<String, Integer> map = new HashMap<>();
        for (int i = 0; i < jarr.size(); i++) {
            JsonObject current = jarr.getJsonObject(i);
            map.put(current.getString(NAME), current.getInteger(WIDTH));
        }

        return map;
    }

    private JsonArray mapSizesToJsonArray(@Nonnull HashMap<String, Integer> map) {
        JsonArray jarr = new JsonArray();
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry) it.next();
            jarr.add(
                    new JsonObject()
                            .put(NAME, pair.getKey())
                            .put(WIDTH, pair.getValue())
            );
            it.remove(); // avoids a ConcurrentModificationException
        }

        return jarr;
    }
}
