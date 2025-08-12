package com.rinha.backend.config;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ConfigLoader {
  public static Future<JsonObject> load(Vertx vertx) {
    ConfigStoreOptions fileStore = new ConfigStoreOptions()
      .setType("file")
      .setFormat("json")
      .setConfig(new JsonObject().put("path", "config/config.json"));

    ConfigStoreOptions devStore = new ConfigStoreOptions()
      .setType("file")
      .setFormat("json")
      .setConfig(new JsonObject().put("path", "dev-config.json"))
      .setOptional(true);

    ConfigStoreOptions envStore = new ConfigStoreOptions().setType("env");

    ConfigRetrieverOptions options = new ConfigRetrieverOptions()
      .addStore(fileStore)
      .addStore(devStore)
      .addStore(envStore);

    ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

    return retriever.getConfig();
  }
}
