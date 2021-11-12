package se.kry.servicepooler.config;

public enum ParamsConfig {

  HTTP_PORT("http.port"),
  FLYWAY_INIT_LOCATION("flyway.locations"),

  MYSQL_HOST_IP("mysql.host.ip"),
  MYSQL_HOST_PORT("mysql.host.port"),
  MYSQL_DB_NAME("mysql.db.name"),
  MYSQL_DB_CREATE_IFNOT_EXIST("mysql.db.createDatabaseIfNotExist"),
  MYSQL_DB_ALLOW_KEY_RETRIVAL("mysql.db.allowPublicKeyRetrieval"),
  MYSQL_DB_USE_SSL("mysql.db.useSSL"),
  MYSQL_DB_USER("mysql.db.user"),
  MYSQL_DB_PASSWORD("mysql.db.password"),
  MYSQL_DB_MAX_POOL_SIZE("mysql.db.max.pool.size"),
  POLLER_ENDPOINT_POLLING_INTERVAL("poller.endpoint.polling.interval"),
  POLLER_DB_POLLING_INTERVAL("poller.db.polling.interval"),
  POLLER_WEB_CLIENT_MAX_POLL_SIZE("poller.web.client.max.poll.size"),
  POLLER_POLLING_TIMEOUT("poller.polling.timeout");

  private final String label;

  ParamsConfig(final String label) {
    this.label = label;
  }

  public String label() {
    return this.label;
  }
}
