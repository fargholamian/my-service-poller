package se.kry.servicepooler.dao;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.*;
import se.kry.servicepooler.util.Utils;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

public class ServiceDaoVerticle extends DaoAbstractVerticle {
  public static final Logger LOGGER = LoggerFactory.getLogger(ServiceDaoVerticle.class);
  private MySQLPool client;
  private HashMap<SqlQuery, String> sqlQueries;

  public ServiceDaoVerticle(Vertx vertx) {
    super(vertx);
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    LOGGER.info("Starting Service DAO Verticle...");
    initializeDB();

    sqlQueries = loadSqlQueries();
    client = MySQLPool.pool(vertx, connectOptions, poolOptions);

    vertx.eventBus().consumer("dao://service/add", this::add);
    vertx.eventBus().consumer("dao://service/modify", this::modify);
    vertx.eventBus().consumer("dao://service/delete", this::delete);
    vertx.eventBus().consumer("dao://service/findAll", this::findAll);
    vertx.eventBus().consumer("dao://service/update", this::updateStatus);
    startPromise.complete();
  }

  public void add(Message<JsonObject> msg) {

    JsonArray jsonArray = new JsonArray() {{
      add(msg.body().getValue("name"));
      add(msg.body().getValue("url"));
      add(Utils.Status.UNKNOWN);
      add(LocalDateTime.now().toString());
      add(LocalDateTime.now().toString());
      add(LocalDateTime.now().toString());
    }};

    client.preparedQuery(sqlQueries.get(SqlQuery.ADD))
      .execute(Tuple.from(jsonArray.stream().toArray())).onComplete(
        res -> {
          if (res.succeeded()) {
            List<JsonObject> list = convertRowSetToJsonArray(res.result());
            msg.reply(new JsonArray(list));
          } else {
            LOGGER.error(res.cause());
          }
        }
      );

  }

  public void modify(Message<JsonObject> msg) {

    JsonArray jsonArray = new JsonArray() {{
      add(msg.body().getValue("url"));
      add(msg.body().getValue("name"));
      add(LocalDateTime.now().toString());
      add(msg.body().getValue("id"));
    }};

    client.preparedQuery(sqlQueries.get(SqlQuery.MODIFY))
      .execute(Tuple.from(jsonArray.stream().toArray())).onComplete(
        res -> {
          if (res.succeeded()) {
            List<JsonObject> list = convertRowSetToJsonArray(res.result());
            msg.reply(new JsonArray(list));
          } else {
            LOGGER.error(res.cause());
          }
        }
      );

  }

  public void updateStatus(Message<JsonObject> msg) {
    LOGGER.debug("Update Status Required:" + msg.body().stream().toString());
    JsonArray jsonArray = new JsonArray() {{
      add(msg.body().getValue("status"));
      add(msg.body().getValue("updated_time"));
      add(msg.body().getValue("id"));
      add(msg.body().getValue("updated_time"));
    }};

    client.preparedQuery(sqlQueries.get(SqlQuery.UPDATE))
      .execute(Tuple.from(jsonArray.stream().toArray())).onComplete(
        res -> {
          if (res.succeeded()) {
            LOGGER.debug("Update Status Done:" + msg.body().stream().toString());
          } else {
            LOGGER.error("Update Status Failed:" + res.cause());
          }
        }
      );

  }

  public void delete(Message<JsonObject> msg) {
    LOGGER.debug("Deleting record:" + msg.body().stream().toString());
    JsonArray jsonArray = new JsonArray() {{
      add(msg.body().getValue("id"));
    }};

    client.preparedQuery(sqlQueries.get(SqlQuery.DELETE))
      .execute(Tuple.from(jsonArray.stream().toArray())).onComplete(
        res -> {
          if (res.succeeded()) {
            List<JsonObject> list = convertRowSetToJsonArray(res.result());
            msg.reply(new JsonArray(list));
            LOGGER.debug("Delete Done:" + msg.body().stream().toString());
          } else {
            LOGGER.error("Delete Failed:" + res.cause());
          }
        }
      );

  }

  public void findAll(Message<JsonObject> msg) {
    LOGGER.debug("Fnding all records:" + msg.body().stream().toString());
    client.getConnection(con -> {

      if (con.succeeded()) {
        SqlConnection sqlConnection = con.result();

        sqlConnection.query(sqlQueries.get(SqlQuery.ALL)).execute().onComplete(res -> {
          if (res.succeeded()) {
            List<JsonObject> list = convertRowSetToJsonArray(res.result());
            msg.reply(new JsonArray(list));
            LOGGER.debug("Finding all records done:" + msg.body().stream().toString());
          } else {
            LOGGER.error("Finding all records failed:" + res.cause());
          }
        });
        con.result().close();
      }

    });
  }

  public HashMap<SqlQuery, String> loadSqlQueries() throws IOException {

    HashMap<SqlQuery, String> sqlQueries = new HashMap<>();
    sqlQueries.put(SqlQuery.ALL, "SELECT C_ID, C_URL, C_NAME, C_STATUS, C_CREATED_TIME, C_UPDATED_TIME, C_MODIFIED_TIME FROM T_SERVICES");
    sqlQueries.put(SqlQuery.ADD, "INSERT INTO T_SERVICES (C_ID, C_NAME, C_URL, C_STATUS, C_CREATED_TIME, C_MODIFIED_TIME, C_UPDATED_TIME) VALUES (NULL, ?, ?, ?, ?, ?, ?)");
    sqlQueries.put(SqlQuery.MODIFY, "UPDATE T_SERVICES SET C_URL=?, C_NAME=? , C_MODIFIED_TIME=? WHERE C_ID=?");
    sqlQueries.put(SqlQuery.UPDATE, "UPDATE T_SERVICES SET C_STATUS=? , C_UPDATED_TIME=? WHERE C_ID=? and (C_UPDATED_TIME<? or c_updated_time is NULL)");
    sqlQueries.put(SqlQuery.DELETE, "DELETE FROM T_SERVICES WHERE C_ID = ?");
    return sqlQueries;
  }

  public enum SqlQuery {
    ALL, ADD, MODIFY, DELETE, UPDATE
  }

}
