package profiles.verticles;

import com.amazonaws.regions.Regions;
import profiles.model.Config;
import profiles.model.ConfigMessageCodec;
import profiles.model.OriginID;
import profiles.model.OriginIDCodec;
import profiles.utility.ImageResize;
import profiles.utility.S3client;
import vertx.common.MicroserviceVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static profiles.verticles.ConfigurationVerticle.EBA_CONFIG_FETCH;
import static profiles.verticles.ConfigurationVerticle.EBA_CONFIG_UPDATE;

/**
 * Verticle that replies to Profile requests
 */
public class ScaleVerticle extends MicroserviceVerticle {

    // Addresses

    public static final String EBA_DELETE_ORIGIN = "delete:origin";
    public static final String EBA_SCALE_ORIGIN = "put:origin";

    // very sensible data

    private static final String ACCESS_KEY = System.getenv("AWS_S3_ACCESS_KEY");
    private static final String SECRET_KEY = System.getenv("AWS_S3_SECRET_KEY");

    // data to be retrieved from config

    private String mPhotosBucket;
    private String mUserpicsBucket;
    private Regions mRegion = Regions.US_EAST_2;
    private String mExtension = "jpg";
    private HashMap<String, Integer> mSizes;
    private S3client mS3Client;

    // Overrides

    @Override
    public void start(Promise<Void> startPromise) {
        createServiceDiscovery();
        registerCodecs();
        setupConfigListener();
        setupConfig(startPromise);
        setupScaleListeners();
    }

    // Private

    private void setupFromConfig(@Nonnull Config config) {
        mPhotosBucket = config.getPhotosBucket();
        mUserpicsBucket = config.getUserpicBucket();
        mRegion = config.getRegion();
        mExtension = config.getExtension();
        mSizes = config.getSizes();
        mS3Client = setupS3Client();
    }

    private S3client setupS3Client() throws NullPointerException {
        if (ACCESS_KEY == null || SECRET_KEY == null || mRegion == null) {
            throw new NullPointerException("Environment variables not settled up to it's values!");
        }
        return new S3client(ACCESS_KEY, SECRET_KEY, mRegion);
    }

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

    private void deletionHandler(String photoID, String bucketName) {
        Iterator it = mSizes.entrySet().iterator();
        ArrayList<String> scaleURLs = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            scaleURLs.add(photoID + "." + pair.getKey());
            it.remove(); // avoids a ConcurrentModificationException
        }
        mS3Client.multiDelete(bucketName, scaleURLs);
    }

    private void scalingHandler(String photoID, String bucketName) throws IOException {
        File orig = mS3Client.download(bucketName, photoID);
        Iterator it = mSizes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            mS3Client.upload(bucketName, ImageResize.resizeFromFile(orig, mExtension, (Integer) pair.getValue()), photoID + "." + pair.getKey());
            it.remove(); // avoids a ConcurrentModificationException
        }
    }

    private void setupScaleListeners() {
        // resize and put to s3
        vertx.eventBus().<OriginID>consumer(EBA_SCALE_ORIGIN, handler -> {
            OriginID url = handler.body();
            final String currentBucket = url.getType() == OriginID.photoType.USERPIC ?
                    mUserpicsBucket : mPhotosBucket;

            vertx.executeBlocking(promise -> {
                try {
                    scalingHandler(url.getID(), currentBucket);
                    promise.complete("OK");
                } catch (IOException e) {
                    // report error to sagas
                    promise.fail("ERR");
                }
            }, false, ar -> {
                if (ar.succeeded()) handler.reply("OK");
                else handler.fail(-1, "Can't download original image");
            });
        });


        // remove scales from s3
        vertx.eventBus().<OriginID>consumer(EBA_DELETE_ORIGIN, handler -> {
            OriginID url = handler.body();
            final String currentBucket = url.getType() == OriginID.photoType.USERPIC ?
                    mUserpicsBucket : mPhotosBucket;

            vertx.executeBlocking(promise -> {
                deletionHandler(url.getID(), currentBucket);
                promise.complete("");
            }, false, ar -> handler.reply("OK"));
        });
    }

    /**
     * Listen on configuration changes and update sizes accordingly
     */
    private void setupConfigListener() {
        vertx.eventBus().<Config>consumer(EBA_CONFIG_UPDATE, configAr -> {
            setupFromConfig(configAr.body());
            System.out.println("New sizes came up: " + mSizes);
        });
    }

    private void setupConfig(Promise<Void> startPromise) {
        Promise<Config> promise = Promise.promise();
        promise.future().setHandler(configAr -> {
            if (configAr.failed()) {
                System.out.println("Scales can't be fetched correctly: " + configAr.cause().getMessage());
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
