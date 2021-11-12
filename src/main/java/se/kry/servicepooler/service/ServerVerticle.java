package se.kry.servicepooler.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import se.kry.servicepooler.config.ParamsConfig;

public class ServerVerticle extends AbstractVerticle {
  public static final Logger LOGGER = LoggerFactory.getLogger(ServerVerticle.class);

  private Router router;

  public ServerVerticle(Vertx vertx) {
    this.vertx = vertx;
    this.router = Router.router(vertx);
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    LOGGER.info("Starting Server Verticle...");
    router.route().handler(BodyHandler.create());
    defaultRoute();
    add();
    modify();
    delete();
    findAll();
    runHttpServer( config().getInteger(ParamsConfig.HTTP_PORT.label() , 7000));
    //startPromise.complete();
  }

  public void runHttpServer(Integer port) {
    HttpServer server = vertx.createHttpServer();
    server.requestHandler(router).listen( port );
  }

  public void defaultRoute() {
    router.route("/*").handler(StaticHandler.create());
  }

  public void add() {

    router.post("/service").handler(routingContext -> {

      JsonObject jsonBody = routingContext.getBodyAsJson();

      LOGGER.debug("New add service request received: " + jsonBody.toString());
      routingContext.vertx().eventBus().<JsonArray>request("dao://service/add", jsonBody, reply -> {
        if (reply.succeeded()) {
          replyMsg(routingContext, reply.result().body(), true);
          LOGGER.debug("Successful response to add service request: " + jsonBody.toString());
        } else {
          replyMsg(routingContext, new JsonArray() , false);
          LOGGER.error(reply.cause());
        }

      });
    });
  }

  public void modify() {
    router.patch("/service").handler(routingContext -> {

      JsonObject jsonBody = routingContext.getBodyAsJson();

      LOGGER.debug("New modify service request received" + jsonBody.toString());
      routingContext.vertx().eventBus().<JsonArray>request("dao://service/modify", jsonBody, reply -> {
        if (reply.succeeded()) {
          replyMsg(routingContext, reply.result().body(), true);
          LOGGER.debug("Successful response to modify service request: " + jsonBody.toString());
        } else {
          replyMsg(routingContext, new JsonArray() , false);
          LOGGER.error(reply.cause());
        }

      });
    });

  }

  public void delete() {
    router.delete("/service").handler(routingContext -> {

      JsonObject jsonBody = routingContext.getBodyAsJson();

      LOGGER.debug("New delete request received" + jsonBody.toString());
      routingContext.vertx().eventBus().<JsonArray>request("dao://service/delete", jsonBody, reply -> {
        if (reply.succeeded()) {
          replyMsg(routingContext, reply.result().body(), true);
          LOGGER.debug("Successful response to delete service request: " + jsonBody.toString());
        } else {
          replyMsg(routingContext, new JsonArray() , false);
          LOGGER.error(reply.cause());
        }
      });
    });

  }

  public void findAll() {
    router.route("/services").handler(routingContext -> {
      LOGGER.debug("New get services request received");
      JsonObject body = new JsonObject();
      routingContext.vertx().eventBus().<JsonArray>request("dao://service/findAll", body, reply -> {
        if (reply.succeeded()) {
          replyMsg(routingContext, reply.result().body(), true);
          LOGGER.debug("Successful response to get services request");
        } else {
          replyMsg(routingContext, new JsonArray() , false);
          LOGGER.error(reply.cause());
        }

      });
    });
  }

  private void replyMsg(RoutingContext routingContext, JsonArray message, Boolean isSuccess) {
    JsonObject respBody = new JsonObject();
    respBody.put( "result" , isSuccess);
    respBody.put( "message" , message);
    routingContext.response().putHeader("content-type", "text/plain;charset=utf-8");
    routingContext.response().end(respBody.encode());
  }

}
