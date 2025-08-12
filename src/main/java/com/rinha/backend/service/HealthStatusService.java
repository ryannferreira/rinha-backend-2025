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
    boolean isDefaultHealthy = isProcessorHealthy("default");
    boolean isFallbackHealthy = isProcessorHealthy("fallback");

    if (isDefaultHealthy && isFallbackHealthy) {
      //Ambos estão saudáveis, desempata pelo menor tempo de resposta
      int defaultTime = statuses.get("default").getInteger("minResponseTime");
      int fallbackTime = statuses.get("fallback").getInteger("minResponseTime");

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
