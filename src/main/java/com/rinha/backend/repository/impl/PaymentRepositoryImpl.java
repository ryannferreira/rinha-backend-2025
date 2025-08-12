package com.rinha.backend.repository.impl;

import com.rinha.backend.model.Payment;
import com.rinha.backend.repository.PaymentRepository;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.time.OffsetDateTime;

public class PaymentRepositoryImpl implements PaymentRepository {
  private final Pool pool;

  public PaymentRepositoryImpl(Pool pool) {
    this.pool = pool;
  }

  @Override
  public Future<Void> save(Payment payment) {
    return pool.withTransaction(client ->
      client.preparedQuery("INSERT INTO payments (correlation_id, amount, processor) VALUES ($1, $2, $3)")
        .execute(Tuple.of(payment.correlationId(), payment.amount(), payment.processor()))
    ).mapEmpty();
  }

  @Override
  public Future<JsonObject> getSummary(String from, String to) {
    StringBuilder sql = new StringBuilder("SELECT processor, COUNT(*) as total_requests, SUM(amount) as total_amount FROM payments");
    Tuple params = Tuple.tuple();
    int paramIndex = 1;

    if (from != null || to != null) {
      sql.append(" WHERE 1=1");
      if (from != null) {
        sql.append(" AND created_at >= $").append(paramIndex++);
        params.addOffsetDateTime(OffsetDateTime.parse(from));
      }
      if (to != null) {
        sql.append(" AND created_at <= $").append(paramIndex++);
        params.addOffsetDateTime(OffsetDateTime.parse(to));
      }
    }

    sql.append(" GROUP BY processor");

    return pool.preparedQuery(sql.toString())
      .execute(params)
      .map(rows -> {JsonObject summary = new JsonObject()
        .put("default", new JsonObject().put("totalRequests", 0L).put("totalAmount", 0.0))
        .put("fallback", new JsonObject().put("totalRequests", 0L).put("totalAmount", 0.0));

        for (Row row : rows) {
          summary.getJsonObject(row.getString("processor"))
            .put("totalRequests", row.getLong("total_requests"))
            .put("totalAmount", row.getNumeric("total_amount"));
        }
        return summary;
      });
  }
}
