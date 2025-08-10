package com.rinha.backend.service.impl;

import com.rinha.backend.model.Payment;
import com.rinha.backend.repository.PaymentRepository;
import com.rinha.backend.service.PaymentService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class PaymentServiceImpl implements PaymentService {

  private final PaymentRepository repository;

  public PaymentServiceImpl(PaymentRepository repository) {
    this.repository = repository;
  }

  @Override
  public Future<JsonObject> createPayment(JsonObject paymentData) {
    try {
      Payment payment = paymentData.mapTo(Payment.class);

      return repository.save(payment);
    } catch (Exception e) {

      return Future.failedFuture(e);
    }
  }

  @Override
  public Future<JsonObject> getSummary() {
    return repository.getSummary();
  }
}
