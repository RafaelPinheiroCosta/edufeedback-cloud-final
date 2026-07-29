package br.com.edufeedback.api.diagnostic;

public record DiagnosticErrorResponse(
    String code,
    String errorClass,
    String message,
    String rootCauseClass,
    String rootCauseMessage) {

  public static DiagnosticErrorResponse from(String code, Throwable failure) {
    Throwable root = rootCause(failure);
    return new DiagnosticErrorResponse(
        code,
        failure == null ? "unknown" : failure.getClass().getName(),
        safeMessage(failure),
        root == null ? "unknown" : root.getClass().getName(),
        safeMessage(root));
  }

  private static Throwable rootCause(Throwable failure) {
    if (failure == null) {
      return null;
    }
    Throwable current = failure;
    while (current.getCause() != null && current.getCause() != current) {
      current = current.getCause();
    }
    return current;
  }

  private static String safeMessage(Throwable failure) {
    if (failure == null || failure.getMessage() == null || failure.getMessage().isBlank()) {
      return "Sem mensagem detalhada.";
    }
    String value = failure.getMessage().replace('\n', ' ').replace('\r', ' ').trim();
    return value.length() <= 2000 ? value : value.substring(0, 2000);
  }
}
