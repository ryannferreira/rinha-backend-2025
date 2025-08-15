package com.rinha.backend.config;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

public class PgPoolProvider {

  public static Pool create(Vertx vertx, JsonObject dbConfig) {
    PgConnectOptions connectOptions = new PgConnectOptions()
      .setHost(dbConfig.getString("host"))
      .setPort(dbConfig.getInteger("port"))
      .setDatabase(dbConfig.getString("database"))
      .setUser(dbConfig.getString("user"))
      .setPassword(dbConfig.getString("password"));

    PoolOptions poolOptions = new PoolOptions().setMaxSize(10);

    return Pool.pool(vertx, connectOptions, poolOptions);
  }
}
