
package vertx.common;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

/**
 * Class that launches vertx instance.
 * Command line arguments are: 'run profiles.verticles.MainVerticle -cluster'.
 * Go to profiles/src/main/java/profiles/verticles/MainVerticle.java to see running verticle
 */
public class Launcher extends io.vertx.core.Launcher {

  // Main

  public static void main(String[] args) {
    new Launcher().dispatch(args);
  }


  // Overrides

  @Override
  public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {
    super.beforeDeployingVerticle(deploymentOptions);

    defaultOptions(deploymentOptions);
  }

  // Private

  private void defaultOptions(DeploymentOptions options) {
    if (options.getConfig() != null) return;

    options.setConfig(new JsonObject());
  }
}



