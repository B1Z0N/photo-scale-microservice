package profiles.verticles;

//import images.model.Config;
//import images.model.Image;
//import images.model.ImageMessageCodec;
//import images.services.SaveImageServiceImpl;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
//import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
//import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
//import software.amazon.awssdk.core.sync.RequestBody;
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
//import software.amazon.awssdk.services.s3.model.PutObjectAclRequest;
//import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import vertx.common.MicroserviceVerticle;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

//import static images.verticles.ConfigurationVerticle.CONFIG_FETCH;
//import static images.verticles.ConfigurationVerticle.CONFIG_UPDATE;


import profiles.model.Config;
import profiles.model.ConfigMessageCodec;
import profiles.model.Profile;
import profiles.model.ProfileMessageCodec;
import profiles.services.ProfileServiceImpl;
import vertx.common.MicroserviceVerticle;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;

import javax.annotation.Nonnull;
import java.io.File;

import static profiles.verticles.ConfigurationVerticle.EBA_CONFIG_FETCH;
import static profiles.verticles.ConfigurationVerticle.EBA_CONFIG_UPDATE;


public class ScaleVerticle extends MicroserviceVerticle {

    private static final String AWS_CREDENTIALS = "conf/aws_credentials.json";
    private static final String BUCKET_NAME = "sagas-life-photo-storage";
    public static final String IMAGE_SCALE = "image:scale";

    private VertxServer mServer;

    @Override
    public void start(Promise<Void> startPromise) {
        createServiceDiscovery();
//        registerCodecs();
        setupConfig();
        setupConfigListener();
//        setupListener();
    }


//    private void registerCodecs() {
//        try {
//            vertx.eventBus().registerDefaultCodec(Image.class, new ImageMessageCodec());
//        } catch (IllegalStateException ignored) {
//        }
//    }
//
//
//    private void setupListener() {
//        vertx.eventBus().<Image>consumer(IMAGE_RECEIVE, handler -> {
//            System.out.println("Server got: " + handler.body().getUuid());
//            ByteArrayInputStream inputStream = new ByteArrayInputStream(handler.body().getData());
//
//            String result;
//            try {
//                result = putToS3Bucket(inputStream.readAllBytes(), inputStream.available());
//            } catch (IOException e) {
//                System.out.println("Exception thrown: " + e.getCause().toString());
//                handler.reply("NOT_OK");
//                return;
//            }
//            System.out.println("AWS url is: " + result);
//            handler.reply("OK");
//        });
//    }


//    private String putToS3Bucket(byte[] bytes, long length) throws IOException {
//
//        JsonObject awsAccesKeys = new JsonObject(new String(Files.readAllBytes(Paths.get(AWS_CREDENTIALS))));
//
//        AwsBasicCredentials credentials = AwsBasicCredentials.create(
//                awsAccesKeys.getString("access_key"),
//                awsAccesKeys.getString("secret_key"));
//
//        S3Client amazonS3 = S3Client.builder().region(Region.EU_NORTH_1)
//                .credentialsProvider(StaticCredentialsProvider.create(credentials)).build();
//
//        //TODO generate uuid from user_id and timestamp
//        String key = UUID.nameUUIDFromBytes(bytes).toString();
//        PutObjectRequest request = PutObjectRequest.builder()
//                .bucket(BUCKET_NAME)
//                .key(key)
//                .contentLength(length)
//                .contentType("image/png").build();
//
//        RequestBody body = RequestBody.fromBytes(bytes);
//        PutObjectAclRequest aclRequest = PutObjectAclRequest.builder().key(key).acl(ObjectCannedACL.PUBLIC_READ).bucket(BUCKET_NAME).build();
//        amazonS3.putObject(request, body);
//        amazonS3.putObjectAcl(aclRequest);
//
////  have to use deprecated client, since there is no way to retreive object's url with new sdk
////        BasicAWSCredentials credentialsDeprecated = new BasicAWSCredentials("AKIAJV7NPC2V43FAKJCA",
////                "Ivx/zFgzkOfTXOmVu+M5ZUeAiwV4olk5bv/mvl8m");
////        AmazonS3 deprecatedClient = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentialsDeprecated)).withRegion("eu-north-1").build();
////    return deprecatedClient.getUrl(bucketName, key).toString();
//        return "https://" + BUCKET_NAME + ".s3.eu-north-1.amazonaws.com/" + key;
//    }


    private void setupConfigListener() {
        vertx.eventBus().<Config>consumer(CONFIG_UPDATE, configAr -> setupServer(configAr.body()).future().setHandler(serverAr -> {
            if (serverAr.failed()) {
                System.out.println("Server restart failed: " + serverAr.cause().getMessage());
            } else {
                System.out.println("Server restarted on " + configAr.body().getEndpointHost() + ":" + configAr.body().getEndpointPort());
            }
        }));
    }

    private void setupConfig() {
        Promise<Config> promise = Promise.promise();
        promise.future().setHandler(configAr -> {
            if (configAr.failed()) {
                System.out.println("Server start failed: " + configAr.cause().getMessage());
            } else {
                System.out.println("Server started on " + configAr.result().getEndpointHost() + ":" + configAr.result().getEndpointPort());
            }
        });
        fetchConfig(promise);
    }

    private void fetchConfig(Promise<Config> promise) {
        vertx.eventBus().<Config>request(CONFIG_FETCH, new JsonObject(), configAr -> {
            if (configAr.failed()) {
                promise.fail(configAr.cause());
                return;
            }

            Config config = configAr.result().body();
            setupServer(config).future().setHandler(serverAr -> {
                if (serverAr.failed()) {
                    promise.fail(serverAr.cause());
                } else {
                    promise.complete(config);
                }
            });
        });
    }

    private Promise<Void> setupServer(@Nonnull Config config) {
        Promise<Void> promise = Promise.promise();

        if (mServer != null) mServer.shutdown();

        mServer = VertxServerBuilder.forAddress(vertx, config.getEndpointHost(), Integer.parseInt(config.getEndpointPort()))
                .useTransportSecurity(certChainFile(config), privateKeyFile(config))
                .addService(new SaveImageServiceImpl(vertx))
                .addService(ProtoReflectionService.newInstance())
                .build()
                .start(ar -> {
                    if (ar.failed()) {
                        promise.fail(ar.cause());
                    } else {
                        publishHttpEndpoint(
                                "Server endpoint",
                                config.getEndpointHost(),
                                Integer.parseInt(config.getEndpointPort()),
                                publishAr -> {
                                });

                        promise.complete();
                    }
                });

        return promise;
    }

    private File certChainFile(@Nonnull Config config) {
        return new File(config.getTlsCertChain());
    }

    private File privateKeyFile(@Nonnull Config config) {
        return new File(config.getTlsPrivKey());
    }
}}