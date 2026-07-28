package br.com.edufeedback.api.exception;

public class BusinessException extends RuntimeException {
  private final String errorCode;

  public BusinessException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public String errorCode() {
    return errorCode;
  }
}
