package com.rinha.backend.handler;

import com.rinha.backend.service.PaymentService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.math.BigDecimal;
import java.util.UUID;


public class PaymentHandler {
  private final PaymentService service;

  public PaymentHandler(PaymentService service) {
    this.service = service;
  }

  public void handlePostPayments(RoutingContext ctx) {
    try {
      JsonObject body = ctx.body().asJsonObject();
      if (body == null || body.getString("correlationId") == null || body.getValue("amount") == null) {
        ctx.response().setStatusCode(400).end("{\"error\":\"Campos obrigatórios ausentes.\"}");
        return;
      }

      UUID.fromString(body.getString("correlationId"));
      new BigDecimal(body.getString("amount"));

      service.createPayment(body)
        .onSuccess(v -> {
          ctx.response()
            .setStatusCode(202)
            .end();
        })
        .onFailure(err -> {
          ctx.response().setStatusCode(500).end("{\"error\":\"Internal server error while accepting payment\"}");
        });

    } catch (Exception e) {
      ctx.response().setStatusCode(400).end("{\"error\":\"Formato de campo inválido: " + e.getMessage() + "\"}");
    }
  }

  public void handleGetSummary(RoutingContext ctx) {
    String from = ctx.queryParam("from").stream().findFirst().orElse(null);
    String to = ctx.queryParam("to").stream().findFirst().orElse(null);

    service.getSummary(from, to)
      .onSuccess(summary -> ctx.response()
        .setStatusCode(200)
        .putHeader("content-type", "application/json")
        .end(summary.encode()))
      .onFailure(ctx::fail);
  }
}
