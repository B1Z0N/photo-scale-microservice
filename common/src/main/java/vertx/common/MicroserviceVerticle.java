package vertx.common;

import io.vertx.core.*;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;

/**
 * Class that defines common verticle's methods to be reused in our future
 * verticles by inheriting from it.
 * Read more to understand code: https://vertx.io/docs/vertx-service-discovery/java/
 * <p>
 * Inheriting from AbstractVerticle to gain access to vertx instance
 */
public class MicroserviceVerticle extends AbstractVerticle {

    protected void vsuccess(String msg) {
        System.out.println(
                String.join(" | ", this.getClass().getName(), "OK", msg)
        );
    }

    protected void verror(String msg) {
        System.out.println(
                String.join(" | ", this.getClass().getName(), "ERR", msg)
        );
    }

    protected void vinfo(String msg) {
        System.out.println(
                String.join(" | ", this.getClass().getName(), "INFO", msg)
        );
    }

    // Private
    protected void createHealthCheck() {
        HealthChecks hc = HealthChecks.create(vertx);
        hc.register("Microservice", 5000, future -> future.complete(Status.OK()));

        HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);
        Router router = Router.router(vertx);
        router.get("/health*").handler(healthCheckHandler);
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(4000, ar -> {
                    if (ar.failed()) {
                        System.out.println("Health check server failed to start: " + ar.cause().getMessage());
                    } else {
                        System.out.println("Health check server started on 4000");
                    }
                });
    }
}

