package br.com.edufeedback.api.problem;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

/** Resposta padronizada no formato RFC 7807, com extensões úteis para observabilidade. */
public record ApiProblem(
    URI type,
    String title,
    int status,
    String detail,
    String instance,
    String errorCode,
    String traceId,
    Instant timestamp,
    Map<String, String> violations) {

  public static ApiProblem of(
      int status, String title, String detail, String instance, String errorCode, String traceId) {
    return new ApiProblem(
        URI.create("about:blank"),
        title,
        status,
        detail,
        instance,
        errorCode,
        traceId,
        Instant.now(),
        Map.of());
  }

  public static ApiProblem validation(
      String detail, String instance, String traceId, Map<String, String> violations) {
    return new ApiProblem(
        URI.create("about:blank"),
        "Dados inválidos",
        400,
        detail,
        instance,
        "VALIDATION_ERROR",
        traceId,
        Instant.now(),
        Map.copyOf(violations));
  }
}
