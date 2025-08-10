package com.rinha.backend.core;

import com.rinha.backend.config.ConfigLoader;
import com.rinha.backend.config.PgPoolProvider;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) {
    ConfigLoader.load(vertx)
      .onSuccess(config -> {
        System.out.println("Configuração carregada.");

        PgPoolProvider.initialize(vertx, config.getJsonObject("database"));

        DeploymentOptions options = new DeploymentOptions().setConfig(config);

        vertx.deployVerticle(new HttpServerVerticle(), options)
          .onSuccess(id -> {
            System.out.println("HttpServerVerticle implantado com sucesso: " + id);
            startPromise.complete();
          })
          .onFailure(startPromise::fail);
      })
      .onFailure(error -> {
        System.err.println("Falha ao carregar a configuração: " + error.getMessage());
        startPromise.fail(error);
      });
  }
}
