package se.kry.servicepooler.config;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ConfigurationManager {

  public static Future<JsonObject> getConfig(Vertx vertx) {
    ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(getConfigOptions()));
    return retriever.getConfig();
  }

  private static ConfigStoreOptions getConfigOptions() {
    return new ConfigStoreOptions()
      .setType("file")
      .setFormat("properties")
      .setConfig(new JsonObject().put("path", "application.properties"));
  }
}
