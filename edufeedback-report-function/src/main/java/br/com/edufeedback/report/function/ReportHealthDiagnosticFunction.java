package br.com.edufeedback.report.function;

import br.com.edufeedback.report.service.WeeklyReportService;
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
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class ReportHealthDiagnosticFunction {
  @Inject ObjectMapper mapper;
  @Inject WeeklyReportService reportService;

  @ConfigProperty(name = "app.admin-email", defaultValue = "")
  String adminEmail;

  @ConfigProperty(name = "app.email.connection-string", defaultValue = "")
  String emailConnection;

  @ConfigProperty(name = "app.email.sender", defaultValue = "")
  String emailSender;

  @ConfigProperty(name = "quarkus.datasource.jdbc.url", defaultValue = "")
  String databaseUrl;

  @ConfigProperty(name = "app.timezone", defaultValue = "")
  String timezone;

  @FunctionName("reportDiagnosticHealth")
  public HttpResponseMessage run(
      @HttpTrigger(
              name = "request",
              methods = {HttpMethod.GET},
              authLevel = AuthorizationLevel.FUNCTION,
              route = "diagnostics/reports/health")
          HttpRequestMessage<Optional<String>> request,
      ExecutionContext context) {
    LocalDate referenceDate = reportService.hoje();
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
        new HealthResponse("UP", "report", referenceDate, timezone, Instant.now(), configuration));
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
      LocalDate referenceDate,
      String timezone,
      Instant timestamp,
      Map<String, Boolean> configuration) {}
}
