package se.kry.servicepooler.dao;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.*;
import se.kry.servicepooler.config.ParamsConfig;
import se.kry.servicepooler.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class DaoAbstractVerticle extends AbstractVerticle {
  protected MySQLConnectOptions connectOptions;
  protected PoolOptions poolOptions;
  protected MySQLPool client;

  public DaoAbstractVerticle(Vertx vertx) {
    this.vertx = vertx;
  }

  public void initializeDB() throws Exception {
    this.connectOptions = new MySQLConnectOptions()
      .setPort(config().getInteger(ParamsConfig.MYSQL_HOST_PORT.label()))
      .setHost(config().getString(ParamsConfig.MYSQL_HOST_IP.label()))
      .setDatabase(config().getString(ParamsConfig.MYSQL_DB_NAME.label()) )
      .setUser(config().getString(ParamsConfig.MYSQL_DB_USER.label()))
      .setPassword(config().getString(ParamsConfig.MYSQL_DB_PASSWORD.label()));

    this.poolOptions = new PoolOptions()
      .setMaxSize(config().getInteger(ParamsConfig.MYSQL_DB_MAX_POOL_SIZE.label()));
  }

  public List<JsonObject> convertRowSetToJsonArray(RowSet<Row> rowSet) {
    if (rowSet == null || rowSet.size() == 0) {
      return new ArrayList<JsonObject>();
    }
    return StreamSupport.stream(rowSet.spliterator(), false)
      .map(r -> new JsonObject()
        .put("id", r.getLong(r.getColumnIndex("C_ID")))
        .put("url", r.getString(r.getColumnIndex("C_URL")))
        .put("name", r.getString(r.getColumnIndex("C_NAME")))
        .put("status", Utils.Status.valueOf(r.getString(r.getColumnIndex("C_STATUS"))))
        .put("created_time", r.getLocalDateTime(r.getColumnIndex("C_CREATED_TIME")).toString())
        .put("modified_time", r.getLocalDateTime(r.getColumnIndex("C_MODIFIED_TIME")).toString())
        .put("updated_time", r.getLocalDateTime(r.getColumnIndex("C_UPDATED_TIME")).toString()))
      .collect(Collectors.toList());
  }

}
