package com.rinha.backend.config;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

public class PgPoolProvider {

  private static Pool pool;

  public static void initialize(Vertx vertx, JsonObject dbConfig) {
    if (pool != null) {
      return;
    }

    PgConnectOptions connectOptions = new PgConnectOptions()
      .setHost(dbConfig.getString("host"))
      .setPort(dbConfig.getInteger("port"))
      .setDatabase(dbConfig.getString("database"))
      .setUser(dbConfig.getString("user"))
      .setPassword(dbConfig.getString("password"));

    PoolOptions poolOptions = new PoolOptions().setMaxSize(5);

    pool = Pool.pool(vertx, connectOptions, poolOptions);
  }

  public static Pool getPool() {
    if (pool == null) {
      throw new IllegalStateException("PgPoolProvider não foi inicializado.");
    }
    return pool;
  }
}
