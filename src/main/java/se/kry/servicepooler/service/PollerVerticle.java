package se.kry.servicepooler.service;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import se.kry.servicepooler.config.ParamsConfig;
import se.kry.servicepooler.util.Utils;

import java.time.LocalDateTime;

public class PollerVerticle extends AbstractVerticle {
  public static final Logger LOGGER = LoggerFactory.getLogger(PollerVerticle.class);
  private Long timeout;

  public PollerVerticle(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    LOGGER.info("Starting Poller Verticle...");
    this.timeout = config().getLong(ParamsConfig.POLLER_POLLING_TIMEOUT.label(), 5000L);
    updateServiceList();
    pollServices();
    vertx.setPeriodic(config().getLong(ParamsConfig.POLLER_DB_POLLING_INTERVAL.label(), 60000L), timerId -> updateServiceList());
    vertx.setPeriodic(config().getLong(ParamsConfig.POLLER_ENDPOINT_POLLING_INTERVAL.label(), 5000L), timerId -> pollServices());
    startPromise.complete();
  }


  private void updateServiceList() {
    LOGGER.debug("Updating Serivce List...");
    JsonObject body = new JsonObject();
    vertx.eventBus().<JsonArray>request("dao://service/findAll", body, reply -> {
      if (reply.succeeded()) {
        SharedData sd = vertx.sharedData();
        LocalMap<String, JsonObject> serviceList = sd.getLocalMap("SERVICE_MAP_LIST");
        reply.result().body()
          .forEach(obj -> {
            JsonObject jsonObject = JsonObject.mapFrom(obj);
            serviceList.put(jsonObject.getString("id"), jsonObject);
          });
      } else {
        LOGGER.error(reply.cause());
      }
    });
  }

  private void pollServices() {
    WebClient client = WebClient.create(vertx, new WebClientOptions().setMaxPoolSize(config().getInteger(ParamsConfig.POLLER_WEB_CLIENT_MAX_POLL_SIZE.label(), 5)));
    SharedData sd = vertx.sharedData();
    LocalMap<String, JsonObject> serviceList = sd.getLocalMap("SERVICE_MAP_LIST");
    LOGGER.debug("Poll Service run again...");

    for (JsonObject jsonObject : serviceList.values()) {
      client.getAbs(jsonObject.getString("url")).timeout(timeout).send(ar -> {
        Utils.Status status = Utils.Status.FAIL;
        LocalDateTime udpateTime = LocalDateTime.now();
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response.statusCode() == 200) {
            status = Utils.Status.OK;
          }
        }
        if (!jsonObject.getString("status").equals(status.name())) {
          JsonObject updatedJsonObject = jsonObject;
          updatedJsonObject.remove("status");
          updatedJsonObject.remove("updated_time");
          updatedJsonObject.put("status", status);
          updatedJsonObject.put("updated_time", udpateTime.toString());
          serviceList.put(jsonObject.getString("id"), updatedJsonObject);
          vertx.eventBus().publish("dao://service/update", updatedJsonObject);
        }
      });
    }
  }
}
