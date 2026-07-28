package br.com.edufeedback.api.exception;

public class InvalidRequestException extends RuntimeException {
  private final String errorCode;

  public InvalidRequestException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public String errorCode() {
    return errorCode;
  }
}
