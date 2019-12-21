package profiles.verticles;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.xspec.S;
import profiles.model.Config;
import profiles.model.ConfigMessageCodec;
import profiles.model.ImageScalesURLs;
import profiles.model.ImageScalesURLsCodec;
import profiles.model.OriginURL;
import profiles.model.OriginURLCodec;
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

    private HashMap<String, Integer> mSizes;
    private S3client mS3Client;

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
        setupS3Client();
        createServiceDiscovery();
        registerCodecs();
        setupConfigListener();
        setupConfig();
        setupScaleListeners();
    }

    // Private

    private void setupS3Client() throws NullPointerException {
        if (ACCESS_KEY == null || SECRET_KEY == null ||
                PHOTOS_BUCKET == null || USERPICS_BUCKET == null) {
            throw new NullPointerException("Environment variables not settled up to it's values!");
        }

        mS3Client = new S3client(ACCESS_KEY, SECRET_KEY, REGION);
    }

    private void registerCodecs() {
        try {
            vertx.eventBus().registerDefaultCodec(OriginURL.class, new OriginURLCodec());
            vertx.eventBus().registerDefaultCodec(ImageScalesURLs.class, new ImageScalesURLsCodec());
            vertx.eventBus().registerDefaultCodec(Config.class, new ConfigMessageCodec());
        } catch (IllegalStateException ignored) {
        }
    }

    private void setupScaleListeners() {
        // resize and put to s3
        vertx.eventBus().<OriginURL>consumer(EBA_SCALE_ORIGIN, handler -> {
            OriginURL url = handler.body();
            String currentBucket = PHOTOS_BUCKET;
            if (url.getType() == OriginURL.photoType.USERPIC) {
                currentBucket = USERPICS_BUCKET;
            }

            try {
                File orig = mS3Client.download(currentBucket, url.getUrl());
                Iterator it = mSizes.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();

                    mS3Client.upload(
                            currentBucket,
                            ImageResize.resizeFromFile(
                                    orig, PHOTO_EXT, (Integer) pair.getValue()
                            ), url.getUrl() + pair.getKey()
                    );

                    it.remove(); // avoids a ConcurrentModificationException
                }
            } catch (IOException e) {
                // report error to sagas
                handler.fail(1, "Can't download original image");
            }
        });

        // remove scales from s3
        vertx.eventBus().<OriginURL>consumer(EBA_DELETE_ORIGIN, handler -> {
            OriginURL url = handler.body();
            String currentBucket = PHOTOS_BUCKET;
            if (url.getType() == OriginURL.photoType.USERPIC) {
                currentBucket = USERPICS_BUCKET;
            }

            Iterator it = mSizes.entrySet().iterator();
            ArrayList<String> scaleURLs = new ArrayList<String>();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();

                scaleURLs.add(url.getUrl() + pair.getKey());

                it.remove(); // avoids a ConcurrentModificationException
            }

            mS3Client.multiDelete(currentBucket, scaleURLs);
        });
    }

    /**
     * Listen on configuration changes and update sizes accordingly
     */
    private void setupConfigListener() {
        vertx.eventBus().<Config>consumer(EBA_CONFIG_UPDATE, configAr -> {
            mSizes = configAr.body().getSizes();
            System.out.println("New sizes came up: " + mSizes);
        });
    }

    private void setupConfig() {
        Promise<Config> promise = Promise.promise();
        promise.future().setHandler(configAr -> {
            if (configAr.failed()) {
                System.out.println("Scales can't be fetched correctly: " + configAr.cause().getMessage());
            } else {
                System.out.println("New sizes came up: " + configAr.result().getSizes().toString());
            }
        });
        fetchConfig(promise);
    }

    /**
     * Get sizes from eventbus and pass it to promise
     */
    private void fetchConfig(Promise<Config> promise) {
        vertx.eventBus().<Config>request(EBA_CONFIG_FETCH, new JsonObject(), configAr -> {
            if (configAr.failed()) {
                promise.fail(configAr.cause());
                return;
            }

            Config config = configAr.result().body();
            mSizes = config.getSizes();
        });
    }
}
