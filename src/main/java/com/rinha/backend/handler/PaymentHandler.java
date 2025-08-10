package com.rinha.backend.handler;

import com.rinha.backend.service.PaymentService;
import io.vertx.ext.web.RoutingContext;


public class PaymentHandler {

  private final PaymentService service;

  public PaymentHandler(PaymentService service) {
    this.service = service;
  }

  public void handlePostPayments(RoutingContext ctx) {
    service.createPayment(ctx.body().asJsonObject())
      .onSuccess(result -> ctx.response()
        .setStatusCode(201)
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(ctx::fail);
  }

  public void handleGetSummary(RoutingContext ctx) {
    service.getSummary()
      .onSuccess(summary -> ctx.response()
        .setStatusCode(200)
        .putHeader("content-type", "application/json")
        .end(summary.encode()))
      .onFailure(ctx::fail);
  }
}
