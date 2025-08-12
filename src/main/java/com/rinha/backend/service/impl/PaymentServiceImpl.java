package com.rinha.backend.service.impl;

import com.rinha.backend.model.Payment;
import com.rinha.backend.repository.PaymentRepository;
import com.rinha.backend.service.HealthStatusService;
import com.rinha.backend.service.PaymentProcessorClient;
import com.rinha.backend.service.PaymentService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentServiceImpl implements PaymentService {
  private final PaymentRepository repository;
  private final PaymentProcessorClient processorClient;
  private final HealthStatusService healthStatusService;

  public PaymentServiceImpl(PaymentRepository repository,
                            PaymentProcessorClient processorClient,
                            HealthStatusService healthStatusService) {
    this.repository = repository;
    this.processorClient = processorClient;
    this.healthStatusService = healthStatusService;
  }

  @Override
  public Future<JsonObject> createPayment(JsonObject paymentData) {
    final UUID correlationId = UUID.fromString(paymentData.getString("correlationId"));
    final BigDecimal amount = new BigDecimal(paymentData.getString("amount"));

    String primaryProcessor = healthStatusService.getBestProcessor();

    if (primaryProcessor == null) {
      return Future.failedFuture("All payment processors are unavailable");
    }

    String secondaryProcessor = "default".equals(primaryProcessor) ? "fallback" : "default";

    return executeAndSavePayment(primaryProcessor, correlationId, amount)
      .recover(error -> {

        if (healthStatusService.isProcessorHealthy(secondaryProcessor)) {
          return executeAndSavePayment(secondaryProcessor, correlationId, amount);
        } else {
          return Future.failedFuture(error);
        }
      })
      .map(processorName -> new JsonObject()
        .put("message", "payment processed by " + processorName)
        .put("processor", processorName));
  }

  private Future<String> executeAndSavePayment(String processorName, UUID correlationId, BigDecimal amount) {
    return processorClient.processPayment(processorName, correlationId, amount)
      .compose(v -> {
        Payment payment = new Payment(correlationId, amount, processorName);
        return repository.save(payment).map(processorName);
      });
  }

  @Override
  public Future<JsonObject> getSummary(String from, String to) {
    return repository.getSummary(from, to);
  }
}
