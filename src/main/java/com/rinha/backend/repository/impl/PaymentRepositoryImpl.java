package com.rinha.backend.repository.impl;

import com.rinha.backend.errors.DuplicatePaymentException;
import com.rinha.backend.model.Payment;
import com.rinha.backend.repository.PaymentRepository;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class PaymentRepositoryImpl implements PaymentRepository {

  private static final String DUPLICATE_KEY_ERROR_CODE = "23505";
  private final Pool pool;

  public PaymentRepositoryImpl(Pool pool) {
    this.pool = pool;
  }

  @Override
  public Future<JsonObject> save(Payment payment) {

    return pool.withTransaction(client ->
      client.preparedQuery("INSERT INTO payments (correlation_id, amount, processor) VALUES ($1, $2, $3)")
        .execute(Tuple.of(payment.correlationId(), payment.amount(), payment.processor()))
    ).recover(Future::failedFuture).mapEmpty();
  }

  @Override
  public Future<JsonObject> getSummary() {
    return pool.query("SELECT processor, COUNT(*) as total_requests, SUM(amount) as total_amount FROM payments GROUP BY processor")
      .execute()
      .map(rows -> {
        JsonObject summary = new JsonObject()
          .put("default", new JsonObject().put("totalRequests", 0L).put("totalAmount", 0.0))
          .put("fallback", new JsonObject().put("totalRequests", 0L).put("totalAmount", 0.0));

        for (Row row : rows) {
          summary.getJsonObject(row.getString("processor"))
            .put("totalRequests", row.getLong("total_requests"))
            .put("totalAmount", row.getNumeric("total_amount").doubleValue());
        }
        return summary;
      });
  }
}
