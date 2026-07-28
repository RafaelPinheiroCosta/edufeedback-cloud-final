package br.com.edufeedback.http.auth;

import br.com.edufeedback.api.exception.AuthenticationException;

public class InvalidCredentialsException extends AuthenticationException {
  public InvalidCredentialsException() {
    super("INVALID_CREDENTIALS", "Usuário ou senha inválidos.");
  }
}
