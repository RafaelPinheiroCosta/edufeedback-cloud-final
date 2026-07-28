package br.com.edufeedback.email;

import br.com.edufeedback.api.exception.ExternalServiceException;

public class EmailDeliveryException extends ExternalServiceException {
  public EmailDeliveryException(String message, Throwable cause) {
    super("EMAIL_DELIVERY_FAILED", message, cause);
  }

  public EmailDeliveryException(String message) {
    this(message, null);
  }
}
