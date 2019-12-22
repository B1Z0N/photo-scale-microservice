package profiles.verticles;

import vertx.common.MicroserviceVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Verticle that deploys all other verticles
 */
public class MainVerticle extends MicroserviceVerticle {
    // Variables
    private Map<String, List<String>> mVerticles = new HashMap<>(); // Overrides

    @Override
    public void start(Promise<Void> startPromise) {
        createServiceDiscovery();
        List<Promise> verticlePromises = Stream.of(
                ServiceDiscoveryVerticle.class,
                ConfigurationVerticle.class)
                .map(el -> redeployVerticle(el.getName(), new JsonObject()))
                .collect(Collectors.toList());

        List<Future> futures = verticlePromises.stream()
                .map((Function<Promise, Future>) Promise::future)
                .collect(Collectors.toList());

        CompositeFuture.all(futures).setHandler(ar -> {
            if (ar.failed()) {
                startPromise.fail(ar.cause());
            }
        }).compose(v -> {
            // When the file is created (fut1), execute this:
            return redeployVerticle(ScaleVerticle.class.getName(), new JsonObject())
                    .future().setHandler(
                            ar -> {
                                if (ar.failed()) {
                                    startPromise.fail(ar.cause());
                                }
                            });
        }).compose(v -> {
            // When the file is created (fut1), execute this:
            return redeployVerticle(ApiVerticle.class.getName(), new JsonObject())
                    .future().setHandler(
                            ar -> {
                                if (ar.failed()) {
                                    startPromise.fail(ar.cause());
                                } else {
                                    startPromise.complete();
                                }
                            });
        });
    }

    // Private
    private Promise<Void> redeployVerticle(String className, JsonObject config) {
        Promise<Void> completion = Promise.promise();
        removeExistingVerticles(className);
        DeploymentOptions options = new DeploymentOptions().setConfig(config);
        vertx.deployVerticle(className, options, ar -> {
            if (ar.failed()) {
                completion.fail(ar.cause());
            } else {
                registerVerticle(className, ar.result());
                completion.complete();
            }
        });
        return completion;
    }

    private void registerVerticle(String className, String deploymentId) {
        mVerticles.computeIfAbsent(className, k -> new ArrayList<>());
        ArrayList<String> configVerticles = (ArrayList<String>) mVerticles.get(className);
        configVerticles.add(deploymentId);
    }

    private void removeExistingVerticles(String className) {
        mVerticles.getOrDefault(className, new ArrayList<>()).forEach(vertx::undeploy);
        mVerticles.remove(className);
    }
}
