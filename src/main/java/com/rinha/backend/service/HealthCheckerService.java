package com.rinha.backend.service;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

public class HealthCheckerService {

  private final Vertx vertx;
  private final WebClient webClient;
  private final HealthStatusService healthStatusService;
  private final String defaultProcessorUrl;
  private final String fallbackProcessorUrl;

  public HealthCheckerService(Vertx vertx, WebClient webClient, HealthStatusService healthStatusService, JsonObject config) {
    this.vertx = vertx;
    this.webClient = webClient;
    this.healthStatusService = healthStatusService;

    JsonObject processorsConfig = config.getJsonObject("processors");
    this.defaultProcessorUrl = processorsConfig.getString("default");
    this.fallbackProcessorUrl = processorsConfig.getString("fallback");
  }

  public void start() {
    vertx.setPeriodic(5100, id -> {
      checkProcessorHealth("default", defaultProcessorUrl);
      checkProcessorHealth("fallback", fallbackProcessorUrl);
    });
  }

  private void checkProcessorHealth(String processorName, String url) {
    webClient
      .getAbs(url + "/payments/service-health")
      .putHeader("Accept", "application/json")
      .putHeader("User-Agent", "middlewarePagamentos/1.0")
      .timeout(1000)
      .send()
      .onSuccess(response -> {
        if (response.statusCode() == 200) {
          JsonObject status = response.bodyAsJsonObject();

          healthStatusService.updateStatus(processorName, status);

        } else {
          reportFailure(processorName, "Status code " + response.statusCode());
        }
      })
      .onFailure(err -> {
        reportFailure(processorName, err.getMessage());
      });
  }

  private void reportFailure(String processorName, String reason) {
    healthStatusService.updateStatus(processorName, new JsonObject().put("failing", true));
  }
}
