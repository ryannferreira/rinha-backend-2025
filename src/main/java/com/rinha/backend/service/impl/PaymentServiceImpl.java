package com.rinha.backend.service.impl;

import com.rinha.backend.repository.PaymentRepository;
import com.rinha.backend.service.PaymentService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentServiceImpl implements PaymentService {
  private final PaymentRepository repository;
  private final Vertx vertx;

  public PaymentServiceImpl(PaymentRepository repository, Vertx vertx) {
    this.repository = repository;
    this.vertx = vertx;
  }

  @Override
  public Future<Void> createPayment(JsonObject paymentData) {
    final UUID correlationId = UUID.fromString(paymentData.getString("correlationId"));
    final BigDecimal amount = new BigDecimal(paymentData.getString("amount"));

    return repository.saveAsPending(correlationId, amount)
            .onSuccess(savedPayment -> {
              JsonObject paymentJson = JsonObject.mapFrom(savedPayment);
              vertx.eventBus().send("payment.process", paymentJson);
            })
            .mapEmpty();
  }

  @Override
  public Future<JsonObject> getSummary(String from, String to) {
    return repository.getSummary(from, to);
  }
}
