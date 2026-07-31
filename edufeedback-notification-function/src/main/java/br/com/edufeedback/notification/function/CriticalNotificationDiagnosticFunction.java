package br.com.edufeedback.notification.function;

import br.com.edufeedback.api.diagnostic.DiagnosticErrorResponse;
import br.com.edufeedback.messaging.FeedbackCriticoEvent;
import br.com.edufeedback.messaging.QueuePublisher;
import br.com.edufeedback.notification.diagnostic.DiagnosticFeedbackPersistence;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class CriticalNotificationDiagnosticFunction {
  @Inject ObjectMapper mapper;
  @Inject DiagnosticFeedbackPersistence persistence;
  @Inject QueuePublisher publisher;

  @FunctionName("enqueueCriticalFeedbackDiagnostic")
  public HttpResponseMessage run(
      @HttpTrigger(
              name = "request",
              methods = {HttpMethod.POST},
              authLevel = AuthorizationLevel.FUNCTION,
              route = "diagnostics/notifications/critical")
          HttpRequestMessage<Optional<String>> request,
      ExecutionContext context) {
    try {
      var input = parse(request.getBody());
      int nota = input.nota() == null ? 0 : input.nota();
      UUID correlationId = UUID.randomUUID();

      var feedback = persistence.create(input.descricao(), nota, correlationId);
      var event =
          new FeedbackCriticoEvent(
              UUID.randomUUID(),
              "FEEDBACK_CRITICAL_DIAGNOSTIC_CREATED",
              correlationId,
              null,
              Instant.now(),
              new FeedbackCriticoEvent.Payload(
                  feedback.feedbackId(), feedback.nota(), feedback.urgencia()));
      publisher.publish(event);

      var response =
          new EnqueueResponse(
              "PUBLICADO",
              event.eventId(),
              feedback.feedbackId(),
              correlationId,
              "/api/diagnostics/notifications/status/"
                  + event.eventId()
                  + "?feedbackId="
                  + feedback.feedbackId(),
              "A avaliação foi confirmada e a mensagem foi publicada em Base64.");
      context
          .getLogger()
          .info(
              "event=diagnostic.notification.enqueued eventId="
                  + event.eventId()
                  + " feedbackId="
                  + feedback.feedbackId()
                  + " correlationId="
                  + correlationId);
      return json(request, HttpStatus.ACCEPTED, response);
    } catch (IllegalArgumentException exception) {
      return json(
          request,
          HttpStatus.BAD_REQUEST,
          DiagnosticErrorResponse.from("INVALID_DIAGNOSTIC_REQUEST", exception));
    } catch (Exception exception) {
      context
          .getLogger()
          .severe(
              "event=diagnostic.notification.failed errorClass="
                  + exception.getClass().getName()
                  + " errorMessage="
                  + exception.getMessage());
      return json(
          request,
          HttpStatus.INTERNAL_SERVER_ERROR,
          DiagnosticErrorResponse.from("DIAGNOSTIC_NOTIFICATION_FAILED", exception));
    }
  }

  private CriticalRequest parse(Optional<String> body) throws Exception {
    if (body.isEmpty() || body.get().isBlank()) {
      return new CriticalRequest(null, 0);
    }
    return mapper.readValue(body.get(), CriticalRequest.class);
  }

  private HttpResponseMessage json(HttpRequestMessage<?> request, HttpStatus status, Object body) {
    try {
      return request
          .createResponseBuilder(status)
          .header("Content-Type", "application/json; charset=utf-8")
          .body(mapper.writeValueAsString(body))
          .build();
    } catch (Exception serializationError) {
      return request
          .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
          .header("Content-Type", "application/json; charset=utf-8")
          .body("{\"code\":\"RESPONSE_SERIALIZATION_FAILED\"}")
          .build();
    }
  }

  public record CriticalRequest(String descricao, Integer nota) {}

  public record EnqueueResponse(
      String status,
      UUID eventId,
      UUID feedbackId,
      UUID correlationId,
      String statusPath,
      String acompanhamento) {}
}
