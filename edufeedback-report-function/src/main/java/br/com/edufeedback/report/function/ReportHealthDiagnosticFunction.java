package br.com.edufeedback.report.function;

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

public class ReportHealthDiagnosticFunction {
  @Inject ObjectMapper mapper;

  @ConfigProperty(name = "app.admin-email")
  Optional<String> adminEmail;

  @ConfigProperty(name = "app.email.connection-string")
  Optional<String> emailConnection;

  @ConfigProperty(name = "app.email.sender")
  Optional<String> emailSender;

  @ConfigProperty(name = "quarkus.datasource.jdbc.url")
  Optional<String> databaseUrl;

  @ConfigProperty(name = "app.timezone")
  Optional<String> timezone;

  @FunctionName("reportDiagnosticHealth")
  public HttpResponseMessage run(
      @HttpTrigger(
              name = "request",
              methods = {HttpMethod.GET},
              authLevel = AuthorizationLevel.FUNCTION,
              route = "diagnostics/reports/health")
          HttpRequestMessage<Optional<String>> request,
      ExecutionContext context) {
    var configuration =
        Map.of(
            "databaseConfigured", configured(databaseUrl),
            "adminEmailConfigured", configured(adminEmail),
            "emailConnectionConfigured", configured(emailConnection),
            "emailSenderConfigured", configured(emailSender),
            "timezoneConfigured", configured(timezone));

    return json(
        request,
        HttpStatus.OK,
        new HealthResponse("UP", "report", Instant.now(), configuration));
  }

  private boolean configured(Optional<String> value) {
    return value.filter(configured -> !configured.isBlank()).isPresent();
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
      Instant timestamp,
      Map<String, Boolean> configuration) {}
}
