package se.kry.servicepooler;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import se.kry.servicepooler.config.ConfigurationManager;
import se.kry.servicepooler.dao.ServiceChangeHistoryDaoVerticle;
import se.kry.servicepooler.dao.ServiceDaoVerticle;
import se.kry.servicepooler.migration.FlywayInitializer;
import se.kry.servicepooler.service.PollerVerticle;
import se.kry.servicepooler.service.ServerVerticle;

public class MainVerticle extends AbstractVerticle {
  public static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
  DeploymentOptions options = new DeploymentOptions();

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    LOGGER.trace("Starting Main Verticle...");

    ConfigurationManager.getConfig(vertx)
      .onComplete(config -> options.setConfig(config.result()))
      .compose(v -> vertx.deployVerticle(new FlywayInitializer(vertx), options))
      .compose(v -> vertx.deployVerticle(new ServiceDaoVerticle(vertx), options))
      .compose(v -> vertx.deployVerticle(new ServiceChangeHistoryDaoVerticle(vertx), options))
      .compose(v -> vertx.deployVerticle(new PollerVerticle(vertx), options))
      .compose(v -> vertx.deployVerticle(new ServerVerticle(vertx), options))
      .onComplete(ar -> {
        if (ar.succeeded())
          LOGGER.info("Everything is OK. Server is running...");
        else {
          LOGGER.error("Oops!...Something goes wrong. Nothing is running. Because: \n" + ar.cause());
          ar.cause().printStackTrace();
          vertx.deploymentIDs().forEach(vertx::undeploy);
          vertx.close();
        }
        }
      );


  }
}
