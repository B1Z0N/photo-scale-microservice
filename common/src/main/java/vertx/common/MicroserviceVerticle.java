package vertx.common;

import io.vertx.core.*;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.types.MessageSource;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Class that defines common verticle's methods to be reused in our future
 * verticles by inheriting from it.
 * Read more to understand code: https://vertx.io/docs/vertx-service-discovery/java/
 *
 * Inheriting from AbstractVerticle to gain access to vertx instance
 */
public class MicroserviceVerticle extends AbstractVerticle {

  // Variables

  private ServiceDiscovery mDiscovery;
  private Set<Record> mRegisteredRecords = new ConcurrentHashSet<>();

  // Overrides

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

  /**
   * Initialize our ServiceDiscovery object - an instrument
   * to publish and discover various resources
   */
  protected void createServiceDiscovery() {
    JsonObject config = config();
    ServiceDiscoveryOptions opts = new ServiceDiscoveryOptions()
            .setBackendConfiguration(config);
    mDiscovery = ServiceDiscovery.create(vertx, opts);
  }

  /**
   * Publish record - way of describing a service
   */
  private void publish(Record record, Handler<AsyncResult<Void>> completion) {
    mDiscovery.publish(record, ar -> {
      if (ar.succeeded()) mRegisteredRecords.add(record);

      completion.handle(ar.map((Void)null));
    });
  }


  // Public
  /**
   * Http service
   *
   * @param endpoint the service name
   * @param host the service address
   * @param port the service port
   * @param completion function to call upon completion
   * the same logic applies to other methods starting with publish
   */
  protected void publishHttpEndpoint(String endpoint, String host, int port, Handler<AsyncResult<Void>> completion) {
    JsonObject filter = new JsonObject()
      .put("name", endpoint);
    mDiscovery.getRecord(filter, ar -> {
      /** if there are no service with this name running */
      if (!ar.succeeded() || ar.result() == null) {
        /** create record to describe this service */
        Record record = HttpEndpoint.createRecord(endpoint, host, port, "/");
        /** and publish the service */
        publish(record, completion);
      }
    });

  }

  /**
   * MessageSource - for sharing messages
   *
   * @param name the service name
   * @param address the service address
   * @param completionHandler function to call upon completion
   */
  protected void publishMessageSource(String name, String address, Handler<AsyncResult<Void>> completionHandler) {
    JsonObject filter = new JsonObject()
      .put("name", name);
    mDiscovery.getRecord(filter, ar -> {
      if (!ar.succeeded() || ar.result() == null) {
        Record record = MessageSource.createRecord(name, address);
        publish(record, completionHandler);
      }
    });
  }

  /**
   * MessageSource - for sharing messages in different formats and ways,
   * like JSon or just plain Java class
   *
   * @param name the service name
   * @param address the service address
   * @param contentClass type of data to be sent
   * @param completionHandler function to call upon completion
   */
  protected void publishMessageSource(String name, String address, Class<?> contentClass, Handler<AsyncResult<Void>> completionHandler) {
    JsonObject filter = new JsonObject()
      .put("name", name);
    mDiscovery.getRecord(filter, ar -> {
      if (!ar.succeeded() || ar.result() == null) {
        Record record = MessageSource.createRecord(name, address, contentClass);
        publish(record, completionHandler);
      }
    });
  }

  /**
   * Event bus services are service proxies(read here: https://avinetworks.com/glossary/service-proxy/)
   *
   * @param name the service name
   * @param address the service address
   * @param serviceClass service interface(like MyService.class)
   * @param completionHandler function to call upon completion
   */
  protected void publishEventBusService(String name, String address, Class<?> serviceClass, Handler<AsyncResult<Void>> completionHandler) {
    JsonObject filter = new JsonObject()
      .put("name", name);
    mDiscovery.getRecord(filter, ar -> {
      if (!ar.succeeded() || ar.result() == null) {
        Record record = EventBusService.createRecord(name, address, serviceClass);
        publish(record, completionHandler);
      }
    });
  }

  protected ServiceDiscovery getDiscovery() {
    return mDiscovery;
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

  private Promise<Void> unpublish(Record record) {
    mRegisteredRecords.remove(record);

    Promise<Void> unregisteringFuture = Promise.promise();
    mDiscovery.unpublish(record.getRegistration(), unregisteringFuture);

    return unregisteringFuture;
  }

  @Override
  public void stop(Promise<Void> stopFuture) {
    List<Promise> promises = mRegisteredRecords
            .stream()
            .map(this::unpublish)
            .collect(Collectors.toList());

    if (promises.isEmpty()) {
      stopDiscovery(stopFuture);
    } else {
      stopServices(promises, stopFuture);
    }
  }

  private void stopDiscovery(Promise<Void> stopPromise) {
    mDiscovery.close();
    stopPromise.complete();
  }

  private void stopServices(List<Promise> promises, Promise<Void> stopPromise) {
    /** Promise is an action to be performed, or already performed */
    List<Future> futures = promises
      .stream()
      .map((Function<Promise, Future>) Promise::future)
      .collect(Collectors.toList());

    /** Future is the result of this action, you could use it to set
     *  a handler that will be invoked after action performs
     */
    CompositeFuture.all(futures)
      .setHandler(ar -> {
        mDiscovery.close();

        if (ar.failed()) {
          stopPromise.fail(ar.cause());
        } else {
          stopPromise.complete();
        }
      });
  }
}

