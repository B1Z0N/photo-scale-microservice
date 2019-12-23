package scales;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;


/**
 * see common/src/main/java/vertx/common/Launcher.java comments
 */
public class Launcher extends vertx.common.Launcher {

    public static void main(String[] args) {
        new Launcher().dispatch(args);
    }

    @Override
    public void beforeStartingVertx(VertxOptions options) {
        super.beforeStartingVertx(options);

        DeploymentOptions deploymentOptions = new DeploymentOptions();
        deploymentOptions.setConfig(new JsonObject());

        ClusterManager mgr = new HazelcastClusterManager();
        options.setClusterManager(mgr);

        // enable clustering and trust all server certificates
        options.getEventBusOptions()
                .setClustered(true)
                .setTrustAll(true);
    }
}