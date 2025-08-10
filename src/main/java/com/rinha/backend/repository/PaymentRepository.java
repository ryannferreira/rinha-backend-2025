package com.rinha.backend.repository;

import com.rinha.backend.model.Payment;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface PaymentRepository {
  Future<JsonObject> save(Payment payment);
  Future<JsonObject> getSummary();
}
