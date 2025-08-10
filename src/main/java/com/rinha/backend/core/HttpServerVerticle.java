package com.rinha.backend.core;

import com.rinha.backend.handler.PaymentHandler;
import com.rinha.backend.config.PgPoolProvider;
import com.rinha.backend.repository.PaymentRepository;
import com.rinha.backend.repository.impl.PaymentRepositoryImpl;
import com.rinha.backend.service.PaymentService;
import com.rinha.backend.service.impl.PaymentServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class HttpServerVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {
    PaymentRepository repository = new PaymentRepositoryImpl(PgPoolProvider.getPool());
    PaymentService service = new PaymentServiceImpl(repository);
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
        System.out.println("Servidor HTTP iniciado na porta " + server.actualPort());
        startPromise.complete();
      })
      .onFailure(startPromise::fail);
  }
}
