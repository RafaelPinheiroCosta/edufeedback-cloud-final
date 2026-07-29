package br.com.edufeedback.report.function;

import br.com.edufeedback.api.diagnostic.DiagnosticErrorResponse;
import br.com.edufeedback.email.EmailSender;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class ReportEmailDiagnosticFunction {
  @Inject ObjectMapper mapper;
  @Inject EmailSender emailSender;

  @ConfigProperty(name = "app.admin-email")
  String adminEmail;

  @FunctionName("sendReportEmailDiagnostic")
  public HttpResponseMessage run(
      @HttpTrigger(
              name = "request",
              methods = {HttpMethod.POST},
              authLevel = AuthorizationLevel.FUNCTION,
              route = "diagnostics/reports/email")
          HttpRequestMessage<Optional<String>> request,
      ExecutionContext context) {
    try {
      var input = parse(request.getBody());
      String recipient = normalizeRecipient(input.recipient());
      String subject =
          input.subject() == null || input.subject().isBlank()
              ? "Diagnóstico direto da Report Function"
              : input.subject().trim();
      String html =
          input.html() == null || input.html().isBlank()
              ? "<h1>EduFeedback</h1><p>Teste direto do e-mail da Report Function.</p>"
              : input.html();

      var result = emailSender.sendHtml(recipient, subject, html);
      context
          .getLogger()
          .info(
              "event=diagnostic.report.email.succeeded operationId="
                  + result.operationId()
                  + " providerStatus="
                  + result.status());
      return json(
          request,
          HttpStatus.OK,
          new EmailDiagnosticResponse(
              "ENVIADO", result.operationId(), result.status(), mask(recipient)));
    } catch (IllegalArgumentException failure) {
      return json(
          request,
          HttpStatus.BAD_REQUEST,
          DiagnosticErrorResponse.from("INVALID_DIAGNOSTIC_EMAIL_REQUEST", failure));
    } catch (Exception failure) {
      context
          .getLogger()
          .severe(
              "event=diagnostic.report.email.failed errorClass="
                  + failure.getClass().getName()
                  + " errorMessage="
                  + failure.getMessage());
      return json(
          request,
          HttpStatus.INTERNAL_SERVER_ERROR,
          DiagnosticErrorResponse.from("DIAGNOSTIC_REPORT_EMAIL_FAILED", failure));
    }
  }

  private EmailDiagnosticRequest parse(Optional<String> body) throws Exception {
    if (body.isEmpty() || body.get().isBlank()) {
      return new EmailDiagnosticRequest(null, null, null);
    }
    return mapper.readValue(body.get(), EmailDiagnosticRequest.class);
  }

  private String normalizeRecipient(String requested) {
    String recipient = requested == null || requested.isBlank() ? adminEmail : requested.trim();
    if (recipient == null
        || recipient.isBlank()
        || !recipient.contains("@")
        || recipient.contains(" ")) {
      throw new IllegalArgumentException(
          "Informe um recipient válido ou configure ADMIN_EMAIL na Function App.");
    }
    return recipient;
  }

  private String mask(String email) {
    return "***@" + email.substring(email.indexOf('@') + 1);
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

  public record EmailDiagnosticRequest(String recipient, String subject, String html) {}

  public record EmailDiagnosticResponse(
      String status, String operationId, String providerStatus, String recipient) {}
}
