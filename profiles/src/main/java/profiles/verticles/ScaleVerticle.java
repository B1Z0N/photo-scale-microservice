package profiles.verticles;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.xspec.S;
import profiles.model.Config;
import profiles.model.ConfigMessageCodec;
import profiles.model.ImageScalesURLs;
import profiles.model.ImageScalesURLsCodec;
import profiles.model.OriginID;
import profiles.model.OriginIDCodec;
import profiles.utility.ImageResize;
import profiles.utility.S3client;

import vertx.common.MicroserviceVerticle;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

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

    // Constants

    public static final String EBA_SCALE_ORIGIN = "put:origin";
    public static final String EBA_DELETE_ORIGIN = "delete:origin";

    private static final HashMap<String, Integer> mSizes = setupSizes();
    private final S3client mS3Client = setupS3Client();

    // it is very sensible data
    private static final String ACCESS_KEY = System.getenv("AWS_S3_ACCESS_KEY");
    private static final String SECRET_KEY = System.getenv("AWS_S3_SECRET_KEY");
    private static final String PHOTOS_BUCKET = System.getenv("AWS_S3_PHOTOS_BUCKET");
    private static final String USERPICS_BUCKET = System.getenv("AWS_S3_USERPICS_BUCKET");

    private static final String PHOTO_EXT = "jpg";
    private static final Regions REGION = Regions.US_EAST_2;
    // Overrides

    @Override
    public void start(Promise<Void> startPromise) {
        createServiceDiscovery();
        registerCodecs();
//        setupConfigListener();
//        setupConfig();
        setupScaleListeners();
    }

    // Private

    private S3client setupS3Client() throws NullPointerException {
        if (ACCESS_KEY == null || SECRET_KEY == null ||
                PHOTOS_BUCKET == null || USERPICS_BUCKET == null) {
            throw new NullPointerException("Environment variables not settled up to it's values!");
        }

        return new S3client(ACCESS_KEY, SECRET_KEY, REGION);
    }

    private static HashMap<String, Integer> setupSizes() {
        HashMap<String, Integer> sizes = new HashMap<>();
        sizes.put("sm", 480);
        sizes.put("md", 1080);
        sizes.put("lg", 1440);

        return sizes;
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

            mS3Client.upload(
                    bucketName,
                    ImageResize.resizeFromFile(
                            orig, PHOTO_EXT, (Integer) pair.getValue()
                    ), photoID + "." + pair.getKey()
            );

            it.remove(); // avoids a ConcurrentModificationException
        }
    }

    private void setupScaleListeners() {
        // resize and put to s3
        vertx.eventBus().<OriginID>consumer(EBA_SCALE_ORIGIN, handler -> {
            OriginID url = handler.body();
            final String currentBucket = url.getType() == OriginID.photoType.USERPIC ?
                    USERPICS_BUCKET : PHOTOS_BUCKET;

            vertx.executeBlocking(promise -> {
                try {
                    scalingHandler(url.getID(), currentBucket);
                    promise.complete("OK");
                } catch (IOException e) {
                    // report error to sagas
                    handler.fail(1, "Can't download original image");
                    promise.complete("ERR");
                }
            }, false, ar -> {
                if (ar.result() == "OK")
                    handler.reply("OK");
                else
                    handler.fail(-1, "ERR");
            });
        });


        // remove scales from s3
        vertx.eventBus().<OriginID>consumer(EBA_DELETE_ORIGIN, handler -> {
            OriginID url = handler.body();
            final String currentBucket = url.getType() == OriginID.photoType.USERPIC ?
                    USERPICS_BUCKET : PHOTOS_BUCKET;

            vertx.executeBlocking(promise -> {
                deletionHandler(url.getID(), currentBucket);
                promise.complete("");
            }, false, ar -> handler.reply("OK"));
        });
    }
//
//    /**
//     * Listen on configuration changes and update sizes accordingly
//     */
//    private void setupConfigListener() {
//        vertx.eventBus().<Config>consumer(EBA_CONFIG_UPDATE, configAr -> {
//            mSizes = configAr.body().getSizes();
//            System.out.println("New sizes came up: " + mSizes);
//        });
//    }
//
//    private void setupConfig() {
//        Promise<Config> promise = Promise.promise();
//        promise.future().setHandler(configAr -> {
//            if (configAr.failed()) {
//                System.out.println("Scales can't be fetched correctly: " + configAr.cause().getMessage());
//            } else {
//                System.out.println("New sizes came up: " + configAr.result().getSizes().toString());
//            }
//        });
//        fetchConfig(promise);
//    }
//
//    /**
//     * Get sizes from eventbus and pass it to promise
//     */
//    private void fetchConfig(Promise<Config> promise) {
//        vertx.eventBus().<Config>request(EBA_CONFIG_FETCH, new JsonObject(), configAr -> {
//            if (configAr.failed()) {
//                promise.fail(configAr.cause());
//                return;
//            }
//
//            Config config = configAr.result().body();
//            mSizes = config.getSizes();
//        });
//    }
}
