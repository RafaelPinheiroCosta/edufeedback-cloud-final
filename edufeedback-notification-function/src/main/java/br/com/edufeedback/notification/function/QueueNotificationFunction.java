package br.com.edufeedback.notification.function;

import br.com.edufeedback.messaging.FeedbackCriticoEvent;
import br.com.edufeedback.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
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
              connection = "AzureWebJobsStorage")
          String message,
      ExecutionContext context)
      throws Exception {
    var event = mapper.readValue(message, FeedbackCriticoEvent.class);
    context
        .getLogger()
        .info(
            "event=queue.trigger.received eventId="
                + event.eventId()
                + " feedbackId="
                + event.payload().feedbackId()
                + " correlationId="
                + event.correlationId());
    try {
      service.processar(event);
      context
          .getLogger()
          .info(
              "event=queue.trigger.completed eventId="
                  + event.eventId()
                  + " correlationId="
                  + event.correlationId());
    } catch (RuntimeException exception) {
      context
          .getLogger()
          .severe(
              "event=queue.trigger.failed eventId="
                  + event.eventId()
                  + " correlationId="
                  + event.correlationId()
                  + " error="
                  + exception.getClass().getSimpleName());
      throw exception;
    }
  }
}
