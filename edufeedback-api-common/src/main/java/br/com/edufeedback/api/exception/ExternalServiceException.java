package br.com.edufeedback.api.exception;

public class ExternalServiceException extends RuntimeException {
  private final String errorCode;

  public ExternalServiceException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String errorCode() {
    return errorCode;
  }
}
