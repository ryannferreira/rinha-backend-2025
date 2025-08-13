package com.rinha.backend.repository;

import com.rinha.backend.model.Payment;
import com.rinha.backend.model.PaymentStatus;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentRepository {
  Future<Payment> saveAsPending(UUID correlationId, BigDecimal amount);
  Future<Void> updateStatus(UUID paymentId, String processorName, PaymentStatus status);
  Future<JsonObject> getSummary(String from, String to);
}
