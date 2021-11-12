package se.kry.servicepooler.migration;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import se.kry.servicepooler.config.ParamsConfig;

public class FlywayInitializer extends AbstractVerticle {
  public static final Logger LOGGER = LoggerFactory.getLogger(FlywayInitializer.class);

  public FlywayInitializer(final Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    LOGGER.info("Starting Migration Verticle...");

    final String url = getDbUrl();
    final String user = config().getString(ParamsConfig.MYSQL_DB_USER.label());
    final String pass = config().getString(ParamsConfig.MYSQL_DB_PASSWORD.label());
    final String location = config().getString(ParamsConfig.FLYWAY_INIT_LOCATION.label());

    final Promise<Void> promise = Promise.promise();
    this.vertx.executeBlocking(fut -> {
      FluentConfiguration fc = Flyway.configure();
      fc.locations(location);
      fc.dataSource(url, user, pass);
      fc.load().migrate();
      fut.complete();
    }, ret -> {
      if (ret.failed()) {
        startPromise.fail(ret.cause());
      } else
        startPromise.complete();
    });

  }

  private String getDbUrl() {
    return "jdbc:mysql://" +
      config().getString(ParamsConfig.MYSQL_HOST_IP.label()) + ":" +
      config().getString(ParamsConfig.MYSQL_HOST_PORT.label()) + "/" +
      config().getString(ParamsConfig.MYSQL_DB_NAME.label()) + "?" +
      "allowPublicKeyRetrieval=" + config().getBoolean(ParamsConfig.MYSQL_DB_ALLOW_KEY_RETRIVAL.label()) + "&" +
      "createDatabaseIfNotExist=" + config().getString(ParamsConfig.MYSQL_DB_CREATE_IFNOT_EXIST.label()) + "&" +
      "useSSL=" + config().getString(ParamsConfig.MYSQL_DB_USE_SSL.label());
  }
}
