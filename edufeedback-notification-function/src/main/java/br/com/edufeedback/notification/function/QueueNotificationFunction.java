package br.com.edufeedback.notification.function;

import br.com.edufeedback.messaging.FeedbackCriticoEvent;
import br.com.edufeedback.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.QueueTrigger;
import jakarta.inject.Inject;

public class QueueNotificationFunction {
  @Inject NotificationService service;
  @Inject ObjectMapper mapper;

  @FunctionName("feedbackCriticalNotification")
  public void run(
      @QueueTrigger(
              name = "message",
              queueName = "%QUEUE_NAME%",
              connection = "AZURE_STORAGE_CONNECTION_STRING")
          String message,
      @BindingName("Id") String messageId,
      @BindingName("DequeueCount") String dequeueCount,
      ExecutionContext context)
      throws Exception {
    FeedbackCriticoEvent event = null;
    try {
      if (message == null || message.isBlank()) {
        throw new IllegalArgumentException("A mensagem recebida da fila está vazia.");
      }

      event = mapper.readValue(message, FeedbackCriticoEvent.class);
      context
          .getLogger()
          .info(
              "event=queue.trigger.received messageId="
                  + safe(messageId)
                  + " dequeueCount="
                  + safe(dequeueCount)
                  + " eventId="
                  + event.eventId()
                  + " feedbackId="
                  + event.payload().feedbackId()
                  + " correlationId="
                  + event.correlationId());

      service.processar(event);

      context
          .getLogger()
          .info(
              "event=queue.trigger.completed messageId="
                  + safe(messageId)
                  + " dequeueCount="
                  + safe(dequeueCount)
                  + " eventId="
                  + event.eventId()
                  + " correlationId="
                  + event.correlationId());
    } catch (Exception exception) {
      context
          .getLogger()
          .severe(
              "event=queue.trigger.failed messageId="
                  + safe(messageId)
                  + " dequeueCount="
                  + safe(dequeueCount)
                  + " eventId="
                  + (event == null ? "unknown" : event.eventId())
                  + " correlationId="
                  + (event == null ? "unknown" : event.correlationId())
                  + " errorClass="
                  + exception.getClass().getName()
                  + " errorMessage="
                  + safe(exception.getMessage())
                  + " payloadPreview="
                  + preview(message));
      throw exception;
    }
  }

  private String preview(String message) {
    if (message == null) return "<null>";
    String normalized = message.replace('\n', ' ').replace('\r', ' ').trim();
    return normalized.length() <= 300 ? normalized : normalized.substring(0, 300) + "...";
  }

  private String safe(String value) {
    return value == null || value.isBlank() ? "unknown" : value;
  }
}
