package com.rinha.backend.service.impl;

import com.rinha.backend.model.Payment;
import com.rinha.backend.model.PaymentStatus;
import com.rinha.backend.repository.PaymentRepository;
import com.rinha.backend.service.HealthStatusService;
import com.rinha.backend.service.PaymentProcessorClient;
import com.rinha.backend.service.PaymentService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentServiceImpl implements PaymentService {
  private static final Logger LOGGER = LoggerFactory.getLogger(PaymentServiceImpl.class);

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
  public Future<Void> createPayment(JsonObject paymentData) {
    final UUID correlationId = UUID.fromString(paymentData.getString("correlationId"));
    final BigDecimal amount = new BigDecimal(paymentData.getString("amount"));

    return repository.saveAsPending(correlationId, amount)
      .onSuccess(savedPayment -> {
        processAndFinalizePayment(savedPayment);
      })
      .mapEmpty();
  }

  private void processAndFinalizePayment(Payment payment) {
    String primaryProcessor = healthStatusService.getBestProcessor();

    if (primaryProcessor == null) {
      repository.updateStatus(payment.id(), null, PaymentStatus.FAILED);
      return;
    }

    String secondaryProcessor = "default".equals(primaryProcessor) ? "fallback" : "default";

    executeOnProcessor(primaryProcessor, payment)
      .recover(error -> {
        if (healthStatusService.isProcessorHealthy(secondaryProcessor)) {
          return executeOnProcessor(secondaryProcessor, payment);
        }
        return Future.failedFuture(error);
      })
      .onFailure(err ->
        repository.updateStatus(payment.id(), null, PaymentStatus.FAILED)
      );
  }

  private Future<Void> executeOnProcessor(String processorName, Payment payment) {
    LOGGER.error("Processing payment {} on processor {} with amount {}", payment.correlationId(), processorName, payment.amount());

    return processorClient.processPayment(processorName, payment.correlationId(), payment.amount())
      .compose(v ->
        repository.updateStatus(payment.id(), processorName, PaymentStatus.PROCESSED)
      );
  }

  @Override
  public Future<JsonObject> getSummary(String from, String to) {
    return repository.getSummary(from, to);
  }
}
