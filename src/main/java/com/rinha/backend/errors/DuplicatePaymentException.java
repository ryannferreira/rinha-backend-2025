package com.rinha.backend.errors;

public class DuplicatePaymentException extends RuntimeException {
  public DuplicatePaymentException() {
    super("Pagamento duplicado.");
  }
}
