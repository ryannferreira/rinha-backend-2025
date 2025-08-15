package com.rinha.backend.repository.impl;

import com.rinha.backend.model.Payment;
import com.rinha.backend.model.PaymentStatus;
import com.rinha.backend.repository.PaymentRepository;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class PaymentRepositoryImpl implements PaymentRepository {
  private final Pool pool;

  public PaymentRepositoryImpl(Pool pool) {
    this.pool = pool;
  }

  @Override
  public Future<Payment> saveAsPending(UUID correlationId, BigDecimal amount) {
    StringBuilder sql = new StringBuilder();
    sql.append("INSERT INTO payments (correlation_id, amount, status) ");
    sql.append("VALUES ($1, $2, $3) ");
    sql.append("RETURNING id, correlation_id, amount, processor, status, created_at");

    return pool.preparedQuery(sql.toString())
      .execute(Tuple.of(correlationId, amount, PaymentStatus.PENDING.toString()))
      .map(rows -> {
        if (rows.size() == 0) {
          throw new IllegalStateException("Insert failed, no rows returned for correlationId: " + correlationId);
        }
        return mapRowToPayment(rows.iterator().next());
      });
  }

  @Override
  public Future<Void> updateStatus(Long paymentId, String processorName, PaymentStatus status) {
    StringBuilder sql = new StringBuilder();
    sql.append("UPDATE payments SET status = $1, processor = $2 ");
    sql.append("WHERE id = $3");

    return pool.preparedQuery(sql.toString())
      .execute(Tuple.of(status.toString(), processorName, paymentId))
      .mapEmpty();
  }

  private Payment mapRowToPayment(Row row) {
    return new Payment(
      row.getLong("id"),
      row.getUUID("correlation_id"),
      row.getBigDecimal("amount"),
      row.getString("processor"),
      PaymentStatus.valueOf(row.getString("status")),
      row.getOffsetDateTime("created_at")
    );
  }

  @Override
  public Future<JsonObject> getSummary(String from, String to) {
    StringBuilder sql = new StringBuilder("SELECT processor, COUNT(*) as total_requests, SUM(amount) as total_amount FROM payments");
    Tuple params = Tuple.tuple();
    int paramIndex = 1;

    if (from != null || to != null) {
      sql.append(" WHERE 1=1");
      sql.append(" AND status = 'PROCESSED'");
      if (from != null) {
        sql.append(" AND created_at >= $").append(paramIndex++);
        params.addOffsetDateTime(OffsetDateTime.parse(from));
      }
      if (to != null) {
        sql.append(" AND created_at <= $").append(paramIndex++);
        params.addOffsetDateTime(OffsetDateTime.parse(to));
      }
    } else {
      sql.append(" WHERE status = 'PROCESSED'");
    }

    sql.append(" GROUP BY processor");

    return pool.preparedQuery(sql.toString())
      .execute(params)
      .map(rows -> {JsonObject summary = new JsonObject()
        .put("default", new JsonObject().put("totalRequests", 0L).put("totalAmount", 0.0))
        .put("fallback", new JsonObject().put("totalRequests", 0L).put("totalAmount", 0.0));

        for (Row row : rows) {
          String processorName = row.getString("processor");
          if (processorName != null && summary.containsKey(processorName)) {
            summary.getJsonObject(processorName)
              .put("totalRequests", row.getLong("total_requests"))
              .put("totalAmount", row.getNumeric("total_amount"));
          }
        }
        return summary;
      });
  }
}
