package com.rinha.backend.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Payment(
  UUID id,
  UUID correlationId,
  BigDecimal amount,
  String processor,
  PaymentStatus status,
  OffsetDateTime createdAt
) {}
