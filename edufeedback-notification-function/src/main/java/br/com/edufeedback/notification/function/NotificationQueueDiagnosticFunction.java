package br.com.edufeedback.notification.function;

import br.com.edufeedback.api.diagnostic.DiagnosticErrorResponse;
import br.com.edufeedback.messaging.QueueClientFactory;
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
import java.util.Optional;

public class NotificationQueueDiagnosticFunction {
  @Inject ObjectMapper mapper;
  @Inject QueueClientFactory queueFactory;

  @FunctionName("getNotificationQueueDiagnostic")
  public HttpResponseMessage run(
      @HttpTrigger(
              name = "request",
              methods = {HttpMethod.GET},
              authLevel = AuthorizationLevel.FUNCTION,
              route = "diagnostics/notifications/queues")
          HttpRequestMessage<Optional<String>> request,
      ExecutionContext context) {
    try {
      String queueName = queueFactory.queueName();
      String poisonQueueName = queueName + "-poison";
      int activeMessages =
          queueFactory.create(queueName).getProperties().getApproximateMessagesCount();
      int poisonMessages =
          queueFactory.create(poisonQueueName).getProperties().getApproximateMessagesCount();
      return json(
          request,
          HttpStatus.OK,
          new QueueDiagnosticResponse(
              queueName, activeMessages, poisonQueueName, poisonMessages, "base64"));
    } catch (Exception failure) {
      context
          .getLogger()
          .severe(
              "event=diagnostic.notification.queues.failed errorClass="
                  + failure.getClass().getName()
                  + " errorMessage="
                  + failure.getMessage());
      return json(
          request,
          HttpStatus.INTERNAL_SERVER_ERROR,
          DiagnosticErrorResponse.from("DIAGNOSTIC_QUEUE_FAILED", failure));
    }
  }

  private HttpResponseMessage json(HttpRequestMessage<?> request, HttpStatus status, Object body) {
    try {
      return request
          .createResponseBuilder(status)
          .header("Content-Type", "application/json; charset=utf-8")
          .body(mapper.writeValueAsString(body))
          .build();
    } catch (Exception failure) {
      return request
          .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
          .header("Content-Type", "application/json; charset=utf-8")
          .body("{\"code\":\"RESPONSE_SERIALIZATION_FAILED\"}")
          .build();
    }
  }

  public record QueueDiagnosticResponse(
      String queueName,
      int activeMessages,
      String poisonQueueName,
      int poisonMessages,
      String messageEncoding) {}
}
