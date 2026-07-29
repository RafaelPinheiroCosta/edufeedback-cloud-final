package br.com.edufeedback.notification.function;

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
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class NotificationHealthDiagnosticFunction {
  @Inject ObjectMapper mapper;

  @ConfigProperty(name = "app.queue.name", defaultValue = "")
  String queueName;

  @ConfigProperty(name = "app.queue.connection-string", defaultValue = "")
  String storageConnection;

  @ConfigProperty(name = "app.queue.endpoint", defaultValue = "")
  String storageEndpoint;

  @ConfigProperty(name = "app.admin-email", defaultValue = "")
  String adminEmail;

  @ConfigProperty(name = "app.email.connection-string", defaultValue = "")
  String emailConnection;

  @ConfigProperty(name = "app.email.sender", defaultValue = "")
  String emailSender;

  @ConfigProperty(name = "quarkus.datasource.jdbc.url", defaultValue = "")
  String databaseUrl;

  @FunctionName("notificationDiagnosticHealth")
  public HttpResponseMessage run(
      @HttpTrigger(
              name = "request",
              methods = {HttpMethod.GET},
              authLevel = AuthorizationLevel.FUNCTION,
              route = "diagnostics/notifications/health")
          HttpRequestMessage<Optional<String>> request,
      ExecutionContext context) {
    var configuration =
        Map.of(
            "databaseConfigured", configured(databaseUrl),
            "storageConfigured", configured(storageConnection) || configured(storageEndpoint),
            "queueNameConfigured", configured(queueName),
            "adminEmailConfigured", configured(adminEmail),
            "emailConnectionConfigured", configured(emailConnection),
            "emailSenderConfigured", configured(emailSender));

    return json(
        request,
        HttpStatus.OK,
        new HealthResponse("UP", "notification", queueName, Instant.now(), configuration));
  }

  private boolean configured(String value) {
    return value != null && !value.isBlank();
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

  public record HealthResponse(
      String status,
      String function,
      String queueName,
      Instant timestamp,
      Map<String, Boolean> configuration) {}
}
