package br.com.edufeedback.api.exception;

public class AuthenticationException extends RuntimeException {
  private final String errorCode;

  public AuthenticationException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public String errorCode() {
    return errorCode;
  }
}
