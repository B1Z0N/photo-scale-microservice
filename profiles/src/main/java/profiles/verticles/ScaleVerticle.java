package profiles.verticles;

import com.hubrick.vertx.s3.client.S3Client;
import com.hubrick.vertx.s3.client.S3ClientOptions;
import com.hubrick.vertx.s3.model.request.DeleteObjectRequest;
import com.hubrick.vertx.s3.model.request.GetObjectRequest;
import com.hubrick.vertx.s3.model.request.PutObjectRequest;
import io.vertx.core.CompositeFuture;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import profiles.model.Config;
import profiles.model.ConfigMessageCodec;
import profiles.model.OriginID;
import profiles.model.OriginIDCodec;
import profiles.utility.ImageResize;
import vertx.common.MicroserviceVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static profiles.verticles.ConfigurationVerticle.EBA_CONFIG_FETCH;
import static profiles.verticles.ConfigurationVerticle.EBA_CONFIG_UPDATE;

/**
 * Verticle that replies to create scales and delete scales requests
 */
public class ScaleVerticle extends MicroserviceVerticle {

    // Addresses

    static final String EBA_DELETE_ORIGIN = "delete:origin";
    static final String EBA_SCALE_ORIGIN = "put:origin";

    // very sensible data

    private static final String ACCESS_KEY = System.getenv("AWS_S3_ACCESS_KEY");
    private static final String SECRET_KEY = System.getenv("AWS_S3_SECRET_KEY");

    // data to be retrieved from config

    private String mPhotosBucket;
    private String mUserpicsBucket;
    private String mRegion = "us-east-2";
    private String mExtension = "jpg";
    private HashMap<String, Integer> mSizes;
    private S3Client mS3Client;

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
        mUserpicsBucket = config.getUserpicsBucket();
        mRegion = config.getRegion();
        mExtension = config.getExtension();
        mSizes = config.getSizes();
        mS3Client = setupS3Client();
    }

    private S3Client setupS3Client() throws NullPointerException {
        if (ACCESS_KEY == null || SECRET_KEY == null || mRegion == null) {
            verror("Internal error: secret data not settled up");
            throw new NullPointerException("Environment variables not settled up to it's values!");
        }

        S3ClientOptions clientOptions = new S3ClientOptions()
                .setAwsRegion(mRegion)
                .setAwsServiceName("s3")
                .setAwsAccessKey(ACCESS_KEY)
                .setAwsSecretKey(SECRET_KEY);

        return new S3Client(vertx, clientOptions);
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

    // finish.reply() after all proms have been successfully completed
    private void finishAfterAll(ArrayList<Promise<Void>> proms, Message<OriginID> finish) {
        CompositeFuture.all(proms
                .stream()
                .map(Promise::future)
                .collect(Collectors.toList())
        ).setHandler(ar -> {
            if (ar.succeeded()) {
                finish.reply("OK");
            } else {
                finish.fail(-1, ar.cause().getMessage());
            }
        });
    }

    // call some S3 request on data from iteration of Sizes
    // and promise to complete after all the job on this iteration is done
    private ArrayList<Promise<Void>> forEachInSizes(BiConsumer<Map.Entry, Promise<Void>> S3request) {
        Iterator it = mSizes.entrySet().iterator();
        ArrayList<Promise<Void>> proms = new ArrayList<>();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            // create new promise for this task
            Promise<Void> current = Promise.promise();

            S3request.accept(pair, current);

            proms.add(current);
            it.remove(); // avoids a ConcurrentModificationException
        }

        return proms;
    }


    // delete scales from S3
    private void deletionHandler(String photoID, String bucketName, Message<OriginID> finish) {
        ArrayList<Promise<Void>> proms = forEachInSizes(
                (pair, current) ->
                        mS3Client.deleteObject(
                                bucketName,
                                photoID + "." + pair.getKey(),
                                new DeleteObjectRequest(),
                                response -> {
                                    vinfo("Deleting from AWS, " + photoID + " :"
                                            + response.getHeader().getContentType());
                                    // complete one of the promises
                                    current.complete();
                                },
                                Throwable::printStackTrace
                        ));

        finishAfterAll(proms, finish);
    }

    // scale buffer and upload it's scales to S3
    private void uploadScaleFromBuffer(Buffer buff, String bucketName, String photoID, Message<OriginID> finish) {
        try {
            final BufferedImage img = ImageResize.bytesToImage(buff.getBytes());
            ArrayList<Promise<Void>> proms = forEachInSizes(
                    (pair, current) -> {
                        // resize

                        try {
                            Buffer scaled = Buffer.buffer(
                                    ImageResize.imageToBytes(
                                            ImageResize.resizeToWidth(
                                                    img, Integer.parseInt(pair.getValue().toString())
                                            ), mExtension
                                    )
                            );

                            mS3Client.putObject(
                                    bucketName,
                                    photoID + "." + pair.getKey(),
                                    new PutObjectRequest(scaled).withContentType("application/json"),
                                    putResponse -> current.complete(),
                                    Throwable::printStackTrace
                            );
                        } catch (IOException e) {
                            finish.fail(-1, "Scaling error");
                            return;
                        }
                    });

            finishAfterAll(proms, finish);
        } catch (IOException e) {
            finish.fail(-1, "IO error");
            return;
        }
    }

    // download original image from S3
    // and call `uploadScaleFromBuffer` on it's buffer
    private void scalingHandler(String photoID, String bucketName, Message<OriginID> finish) {
        Buffer img = Buffer.buffer();
        mS3Client.getObject(
                bucketName,
                photoID,
                new GetObjectRequest().withResponseContentType("application/json"),
                // get original photo, scale and put it's scales to s3
                getResponse -> {
                    getResponse.getData().handler(img::appendBuffer);
                    getResponse.getData().endHandler(ar -> {
                        uploadScaleFromBuffer(img, bucketName, photoID, finish);
                        vinfo("Scaling, AWS, " + photoID + " :" + getResponse.getHeader().getContentType());
                    });
                },
                Throwable::printStackTrace
        );

    }

    // setup all listeners
    private void setupScaleListeners() {
        // get from s3, resize and put scales to s3
        vertx.eventBus().<OriginID>consumer(EBA_SCALE_ORIGIN, handler -> {
            OriginID url = handler.body();
            final String currentBucket = url.getType() == OriginID.photoType.USERPIC ?
                    mUserpicsBucket : mPhotosBucket;

            scalingHandler(url.getID(), currentBucket, handler);
        });


        // remove scales from s3
        vertx.eventBus().<OriginID>consumer(EBA_DELETE_ORIGIN, handler -> {
            OriginID url = handler.body();
            final String currentBucket = url.getType() == OriginID.photoType.USERPIC ?
                    mUserpicsBucket : mPhotosBucket;

            deletionHandler(url.getID(), currentBucket, handler);
        });
    }

    /**
     * Listen on configuration changes and update sizes accordingly
     */
    private void setupConfigListener() {
        vertx.eventBus().<Config>consumer(EBA_CONFIG_UPDATE, configAr -> {
            setupFromConfig(configAr.body());
            vinfo("New sizes came up: " + mSizes);
        });
    }

    private void setupConfig(Promise<Void> startPromise) {
        Promise<Config> promise = Promise.promise();
        promise.future().setHandler(configAr -> {
            if (configAr.failed()) {
                verror("Config fetch: " + configAr.cause().getMessage());
            } else {
                vsuccess("Config fetch, sizes: " +
                        configAr.result().getKafkaHost() + ":" + configAr.result().getSizes().toString());
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
                verror("Setup");
                return;
            }

            setupFromConfig(configAr.result().body());
            startPromise.complete();
            vsuccess("Setup");
        });
    }
}
