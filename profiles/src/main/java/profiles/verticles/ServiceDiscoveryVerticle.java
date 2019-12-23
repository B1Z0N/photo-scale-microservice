package profiles.verticles;

import vertx.common.MicroserviceVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.servicediscovery.rest.ServiceDiscoveryRestEndpoint;

/**
 * https://vertx.io/docs/vertx-service-discovery/java/
 */
public class ServiceDiscoveryVerticle extends MicroserviceVerticle {

    // Constants

    private static final int DISCOVERY_PORT = 8080;

    // Overrides

    @Override
    public void start(Promise<Void> startPromise) {
        createServiceDiscovery();

        Router router = Router.router(vertx);
        ServiceDiscoveryRestEndpoint.create(router, getDiscovery());

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(DISCOVERY_PORT, ar -> {
                    if (ar.failed()) {
                        System.out.println("Service discovery endpoint failed to start: " + ar.cause().getMessage());
                        startPromise.fail(ar.cause());
                    } else {
                        System.out.println("Service discovery endpoint started on " + DISCOVERY_PORT);
                        startPromise.complete();
                    }
                });
    }
}
