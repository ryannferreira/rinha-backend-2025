package com.rinha.backend.config;

import io.vertx.core.json.JsonObject;
import org.flywaydb.core.Flyway;

public class DbMigration {

  public static void execute(JsonObject dbConfig) {
    String jdbcUrl = String.format(
      "jdbc:postgresql://%s:%d/%s",
      dbConfig.getString("host"),
      dbConfig.getInteger("port"),
      dbConfig.getString("database")
    );

    Flyway flyway = Flyway.configure()
      .dataSource(jdbcUrl, dbConfig.getString("user"), dbConfig.getString("password"))
      .load();

    flyway.migrate();
  }
}
