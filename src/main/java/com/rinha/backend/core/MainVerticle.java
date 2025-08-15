package com.rinha.backend.core;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rinha.backend.config.ConfigLoader;
import com.rinha.backend.config.DbMigration;
import com.rinha.backend.config.PgPoolProvider;
import com.rinha.backend.repository.PaymentRepository;
import com.rinha.backend.repository.impl.PaymentRepositoryImpl;
import com.rinha.backend.service.HealthCheckerService;
import com.rinha.backend.service.HealthStatusService;
import com.rinha.backend.service.PaymentProcessorClient;
import com.rinha.backend.service.PaymentService;
import com.rinha.backend.service.impl.PaymentServiceImpl;
import io.vertx.core.*;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.client.WebClient;
import io.vertx.sqlclient.Pool;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        DatabindCodec.mapper().registerModule(new JavaTimeModule());

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

                    PaymentRepository repository = new PaymentRepositoryImpl(pool);
                    PaymentProcessorClient processorClient = new PaymentProcessorClient(webClient, config);
                    PaymentService paymentService = new PaymentServiceImpl(repository, vertx);

                    int processorInstances = Runtime.getRuntime().availableProcessors();
                    DeploymentOptions processorOptions = new DeploymentOptions()
                            .setConfig(config)
                            .setThreadingModel(ThreadingModel.WORKER)
                            .setInstances(processorInstances);

                    DeploymentOptions httpOptions = new DeploymentOptions().setConfig(config);

                    Future<String> httpVerticleDeployment = vertx.deployVerticle(
                            new HttpServerVerticle(paymentService), httpOptions);

                    Future<String> processorVerticleDeployment = vertx.deployVerticle(
                            new PaymentProcessorVerticle(repository, processorClient, healthStatusService), processorOptions);

                    Future.all(httpVerticleDeployment, processorVerticleDeployment)
                            .onSuccess(v -> startPromise.complete())
                            .onFailure(err -> startPromise.fail(err));
                })
                .onFailure(startPromise::fail);
    }
}
