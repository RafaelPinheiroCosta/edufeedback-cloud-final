package br.com.edufeedback.notification.function;

import br.com.edufeedback.api.diagnostic.DiagnosticErrorResponse;
import br.com.edufeedback.notification.diagnostic.NotificationDiagnosticQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.UUID;

public class NotificationStatusDiagnosticFunction {
  @Inject ObjectMapper mapper;
  @Inject NotificationDiagnosticQueryService queryService;

  @FunctionName("getNotificationDiagnosticStatus")
  public HttpResponseMessage run(
      @HttpTrigger(
              name = "request",
              methods = {HttpMethod.GET},
              authLevel = AuthorizationLevel.FUNCTION,
              route = "diagnostics/notifications/status/{eventId}")
          HttpRequestMessage<Optional<String>> request,
      @BindingName("eventId") String eventIdValue,
      ExecutionContext context) {
    try {
      UUID eventId = UUID.fromString(eventIdValue);
      String feedbackIdValue = request.getQueryParameters().get("feedbackId");
      UUID feedbackId =
          feedbackIdValue == null || feedbackIdValue.isBlank()
              ? null
              : UUID.fromString(feedbackIdValue);
      return json(request, HttpStatus.OK, queryService.consultar(eventId, feedbackId));
    } catch (IllegalArgumentException failure) {
      return json(
          request,
          HttpStatus.BAD_REQUEST,
          DiagnosticErrorResponse.from("INVALID_DIAGNOSTIC_IDENTIFIER", failure));
    } catch (Exception failure) {
      context
          .getLogger()
          .severe(
              "event=diagnostic.notification.status.failed errorClass="
                  + failure.getClass().getName()
                  + " errorMessage="
                  + failure.getMessage());
      return json(
          request,
          HttpStatus.INTERNAL_SERVER_ERROR,
          DiagnosticErrorResponse.from("DIAGNOSTIC_STATUS_FAILED", failure));
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
}
