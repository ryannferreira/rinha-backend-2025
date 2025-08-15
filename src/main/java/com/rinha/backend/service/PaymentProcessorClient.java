package com.rinha.backend.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class PaymentProcessorClient {
  private final WebClient webClient;
  private final String processorDefaultUrl;
  private final String processorFallbackUrl;

  public PaymentProcessorClient(WebClient webClient, JsonObject config) {
    this.webClient = webClient;
    this.processorDefaultUrl = config.getJsonObject("processors").getString("default");
    this.processorFallbackUrl = config.getJsonObject("processors").getString("fallback");
  }

  public Future<Void> processPayment(String processor, UUID correlationId, BigDecimal amount) {
    String targetUrl = "default".equals(processor) ? processorDefaultUrl : processorFallbackUrl;

    JsonObject requestBody = new JsonObject()
      .put("correlationId", correlationId.toString())
      .put("amount", amount)
      .put("requestedAt", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

    return webClient
      .postAbs(targetUrl + "/payments")
      .putHeader("Accept", "application/json")
      .putHeader("User-Agent", "middlewarePagamentos/1.0")
      .timeout(1000)
      .sendJsonObject(requestBody)
      .flatMap(response -> {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
          return Future.succeededFuture();

        } else {
          return Future.failedFuture(
            "Falha no processador: " + processor + " com status " + response.statusCode()
          );
        }
      });
  }
}
