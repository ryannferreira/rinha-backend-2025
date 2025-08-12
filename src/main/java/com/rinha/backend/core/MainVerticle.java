package com.rinha.backend.core;

import com.rinha.backend.config.ConfigLoader;
import com.rinha.backend.config.DbMigration;
import com.rinha.backend.config.PgPoolProvider;
import com.rinha.backend.service.HealthCheckerService;
import com.rinha.backend.service.HealthStatusService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.ext.web.client.WebClient;
import io.vertx.sqlclient.Pool;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {
    ConfigLoader.load(vertx)
      .compose(config ->
        vertx.executeBlocking(() -> {
          DbMigration.execute(config.getJsonObject("database"));
          return config;
        })
      )
      .onSuccess(config -> {
        Pool pool = PgPoolProvider.create(vertx, config.getJsonObject("database"));
        WebClient webClient = WebClient.create(vertx);

        HealthStatusService healthStatusService = new HealthStatusService();

        HealthCheckerService healthChecker = new HealthCheckerService(vertx, webClient, healthStatusService, config);
        healthChecker.start();

        DeploymentOptions options = new DeploymentOptions().setConfig(config);
        HttpServerVerticle httpServer = new HttpServerVerticle(pool, webClient, healthStatusService);

        vertx.deployVerticle(httpServer, options)
          .onSuccess(id -> {
            startPromise.complete();
          })
          .onFailure(startPromise::fail);
      })
      .onFailure(error -> {
        startPromise.fail(error);
      });
  }
}
