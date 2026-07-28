package br.com.edufeedback.messaging;

import java.time.Instant;
import java.util.UUID;

public record FeedbackCriticoEvent(
    UUID eventId,
    String eventType,
    UUID correlationId,
    String traceparent,
    Instant occurredAt,
    Payload payload) {
  public record Payload(UUID feedbackId, short nota, String urgencia) {}
}
