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

import java.io.IOException;
import java.util.HashMap;

public class ServiceChangeHistoryDaoVerticle extends DaoAbstractVerticle {
  public static final Logger LOGGER = LoggerFactory.getLogger(ServiceChangeHistoryDaoVerticle.class);
  private HashMap<SqlQuery, String> sqlQueries;

  public ServiceChangeHistoryDaoVerticle(Vertx vertx) {
    super(vertx);
  }

  public void start(Promise<Void> startPromise) throws Exception {
    LOGGER.info("Starting Service Change History DAO Verticle...");
    initializeDB();

    sqlQueries = loadSqlQueries();
    client = MySQLPool.pool(vertx, connectOptions, poolOptions);

    vertx.eventBus().consumer("dao://service/update", this::add);
    startPromise.complete();

  }

  public void add(Message<JsonObject> msg) {

    JsonArray jsonArray = new JsonArray() {{
      add(msg.body().getValue("id"));
      add(msg.body().getValue("name"));
      add(msg.body().getValue("url"));
      add(msg.body().getValue("status"));
      add(msg.body().getValue("updated_time"));
    }};

    client.preparedQuery(sqlQueries.get(SqlQuery.ADD))
      .execute(Tuple.from(jsonArray.stream().toArray())).onComplete(
        res -> {
          if (res.succeeded()) {
            LOGGER.debug("History Updated: url:" + msg.body().getValue("url") + " status: " + msg.body().getValue("status") );
          } else {
            LOGGER.error(res.cause());
          }
        }
      );
  }

  public HashMap<SqlQuery, String> loadSqlQueries() throws IOException {
    HashMap<SqlQuery, String> sqlQueries = new HashMap<>();
    sqlQueries.put(SqlQuery.ADD, "INSERT INTO T_STATUS_HISTORY (C_ID, C_NAME, C_URL, C_STATUS, C_UPDATED_TIME) VALUES (?, ?, ?, ?, ?)");
    return sqlQueries;
  }

  public enum SqlQuery {
    ADD
  }



}
