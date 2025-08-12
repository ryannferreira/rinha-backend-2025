package com.rinha.backend.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface PaymentService {
  Future<JsonObject> createPayment(JsonObject paymentData);
  Future<JsonObject> getSummary(String from, String to);
}
