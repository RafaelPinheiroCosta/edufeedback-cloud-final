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
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class WeeklyReportDiagnosticFunction {
  @Inject ObjectMapper mapper;
  @Inject WeeklyReportService service;

  @FunctionName("runWeeklyFeedbackReportDiagnostic")
  public HttpResponseMessage run(
      @HttpTrigger(
              name = "request",
              methods = {HttpMethod.POST},
              authLevel = AuthorizationLevel.FUNCTION,
              route = "diagnostics/reports/weekly")
          HttpRequestMessage<Optional<String>> request,
      ExecutionContext context) {
    try {
      var input = parse(request.getBody());
      LocalDate referenceDate =
          input.referenceDate() == null ? service.hoje() : input.referenceDate();

      context.getLogger().info("event=diagnostic.report.started referenceDate=" + referenceDate);
      var result = service.gerar(referenceDate);
      context
          .getLogger()
          .info(
              "event=diagnostic.report.completed reportId="
                  + result.reportId()
                  + " status="
                  + result.status());

      return json(
          request,
          HttpStatus.OK,
          new ReportDiagnosticResponse(
              result.status(), result.reportId(), result.inicio(), result.fim(), referenceDate));
    } catch (IllegalArgumentException exception) {
      return json(
          request,
          HttpStatus.BAD_REQUEST,
          new ErrorResponse("INVALID_DIAGNOSTIC_REQUEST", exception.getMessage()));
    } catch (Exception exception) {
      context
          .getLogger()
          .severe(
              "event=diagnostic.report.failed errorClass="
                  + exception.getClass().getName()
                  + " errorMessage="
                  + exception.getMessage());
      return json(
          request,
          HttpStatus.INTERNAL_SERVER_ERROR,
          new ErrorResponse("DIAGNOSTIC_REPORT_FAILED", exception.getMessage()));
    }
  }

  private ReportDiagnosticRequest parse(Optional<String> body) throws Exception {
    if (body.isEmpty() || body.get().isBlank()) {
      return new ReportDiagnosticRequest(null);
    }
    return mapper.readValue(body.get(), ReportDiagnosticRequest.class);
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

  public record ReportDiagnosticRequest(LocalDate referenceDate) {}

  public record ReportDiagnosticResponse(
      String status, UUID reportId, LocalDate inicio, LocalDate fim, LocalDate referenceDate) {}

  public record ErrorResponse(String code, String message) {}
}
