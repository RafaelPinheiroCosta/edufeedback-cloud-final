package br.com.edufeedback.messaging;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record FeedbackCriticoEvent(
    UUID eventId,
    String eventType,
    UUID correlationId,
    String traceparent,
    Instant occurredAt,
    Payload payload) {

  public FeedbackCriticoEvent {
    Objects.requireNonNull(eventId, "eventId é obrigatório.");
    if (eventType == null || eventType.isBlank()) {
      throw new IllegalArgumentException("eventType é obrigatório.");
    }
    Objects.requireNonNull(correlationId, "correlationId é obrigatório.");
    Objects.requireNonNull(occurredAt, "occurredAt é obrigatório.");
    Objects.requireNonNull(payload, "payload é obrigatório.");
  }

  public record Payload(UUID feedbackId, short nota, String urgencia) {
    public Payload {
      Objects.requireNonNull(feedbackId, "payload.feedbackId é obrigatório.");
      if (nota < 0 || nota > 10) {
        throw new IllegalArgumentException("payload.nota deve estar entre 0 e 10.");
      }
      if (urgencia == null || urgencia.isBlank()) {
        throw new IllegalArgumentException("payload.urgencia é obrigatória.");
      }
      if (!"CRITICA".equalsIgnoreCase(urgencia)) {
        throw new IllegalArgumentException(
            "FeedbackCriticoEvent aceita somente payload.urgencia=CRITICA.");
      }
    }
  }
}
