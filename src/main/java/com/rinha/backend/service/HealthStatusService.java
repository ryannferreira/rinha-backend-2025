package com.rinha.backend.service;

import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HealthStatusService {
  private final Map<String, JsonObject> statuses = new ConcurrentHashMap<>();

  public HealthStatusService() {
    statuses.put("default", new JsonObject().put("failing", false).put("minResponseTime", 1000));
    statuses.put("fallback", new JsonObject().put("failing", false).put("minResponseTime", 1000));
  }

  public void updateStatus(String processor, JsonObject status) {
    statuses.put(processor, status);
  }

  public boolean isProcessorHealthy(String processor) {
    return !statuses.getOrDefault(processor, new JsonObject().put("failing", true)).getBoolean("failing");
  }

  public String getBestProcessor() {
    final JsonObject defaultStatus = statuses.get("default");
    final JsonObject fallbackStatus = statuses.get("fallback");

    final boolean isDefaultHealthy = !defaultStatus.getBoolean("failing");
    final boolean isFallbackHealthy = !fallbackStatus.getBoolean("failing");

    if (isDefaultHealthy && isFallbackHealthy) {
      //Ambos estão saudáveis, desempata pelo menor tempo de resposta
      int defaultTime = defaultStatus.getInteger("minResponseTime");
      int fallbackTime = fallbackStatus.getInteger("minResponseTime");
      return defaultTime <= fallbackTime ? "default" : "fallback";

    } else if (isDefaultHealthy) {
      return "default";

    } else if (isFallbackHealthy) {
      return "fallback";

    } else {
      return null;
    }
  }
}
