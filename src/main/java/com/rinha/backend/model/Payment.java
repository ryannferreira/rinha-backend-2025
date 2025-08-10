package com.rinha.backend.model;

import java.math.BigDecimal;
import java.util.UUID;

public record Payment(UUID correlationId, BigDecimal amount, String processor) {}
