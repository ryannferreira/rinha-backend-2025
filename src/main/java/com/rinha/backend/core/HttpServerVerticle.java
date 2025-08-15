package com.rinha.backend.core;

import com.rinha.backend.handler.PaymentHandler;
import com.rinha.backend.repository.PaymentRepository;
import com.rinha.backend.repository.impl.PaymentRepositoryImpl;
import com.rinha.backend.service.HealthStatusService;
import com.rinha.backend.service.PaymentProcessorClient;
import com.rinha.backend.service.PaymentService;
import com.rinha.backend.service.impl.PaymentServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.sqlclient.Pool;

public class HttpServerVerticle extends AbstractVerticle {
  private final Pool pool;
  private final WebClient webClient;
  private final HealthStatusService healthStatusService;

  public HttpServerVerticle(Pool pool, WebClient webClient, HealthStatusService healthStatusService) {
    this.pool = pool;
    this.webClient = webClient;
    this.healthStatusService = healthStatusService;
  }

  @Override
  public void start(Promise<Void> startPromise) {

    PaymentRepository repository = new PaymentRepositoryImpl(this.pool);
    PaymentProcessorClient processorClient = new PaymentProcessorClient(this.webClient, config());
    PaymentService service = new PaymentServiceImpl(repository, processorClient, this.healthStatusService);
    PaymentHandler handler = new PaymentHandler(service);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router.post("/payments").handler(handler::handlePostPayments);
    router.get("/payments-summary").handler(handler::handleGetSummary);

    int port = config().getJsonObject("http", new JsonObject()).getInteger("port", 8888);

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(port)
      .onSuccess(server -> {
        startPromise.complete();
      })
      .onFailure(startPromise::fail);
  }
}
