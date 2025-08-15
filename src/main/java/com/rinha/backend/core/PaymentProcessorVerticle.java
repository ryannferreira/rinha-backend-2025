package com.rinha.backend.core;

import com.rinha.backend.model.Payment;
import com.rinha.backend.model.PaymentStatus;
import com.rinha.backend.repository.PaymentRepository;
import com.rinha.backend.service.HealthStatusService;
import com.rinha.backend.service.PaymentProcessorClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

public class PaymentProcessorVerticle extends AbstractVerticle {
  private final PaymentRepository repository;
  private final PaymentProcessorClient processorClient;
  private final HealthStatusService healthStatusService;

  public PaymentProcessorVerticle(PaymentRepository repository, PaymentProcessorClient processorClient, HealthStatusService healthStatusService) {
    this.repository = repository;
    this.processorClient = processorClient;
    this.healthStatusService = healthStatusService;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    vertx.eventBus().<JsonObject>consumer("payment.process", message -> {
      Payment payment = message.body().mapTo(Payment.class);

      processAndFinalizePayment(payment);
    });
    startPromise.complete();
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
    return processorClient.processPayment(processorName, payment.correlationId(), payment.amount())
      .compose(v ->
        repository.updateStatus(payment.id(), processorName, PaymentStatus.PROCESSED)
      );
  }
}
