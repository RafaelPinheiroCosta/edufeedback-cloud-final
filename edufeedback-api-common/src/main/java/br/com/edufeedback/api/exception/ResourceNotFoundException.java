package br.com.edufeedback.api.exception;

public class ResourceNotFoundException extends RuntimeException {
  private final String errorCode;

  public ResourceNotFoundException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public String errorCode() {
    return errorCode;
  }
}
