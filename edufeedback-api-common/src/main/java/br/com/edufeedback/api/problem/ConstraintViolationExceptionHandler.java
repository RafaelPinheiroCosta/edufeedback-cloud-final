package br.com.edufeedback.api.problem;

import jakarta.annotation.Priority;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

@Provider
@Priority(Priorities.USER - 1)
public class ConstraintViolationExceptionHandler
    implements ExceptionMapper<ConstraintViolationException> {

  private static final Logger LOG = Logger.getLogger(ConstraintViolationExceptionHandler.class);

  private static final String PROBLEM_JSON = "application/problem+json";

  @Context UriInfo uriInfo;

  @Override
  public Response toResponse(ConstraintViolationException exception) {
    String instance = requestPath();
    String traceId = traceId();

    ApiProblem problem =
        ApiProblem.validation(
            "A requisição contém campos inválidos.", instance, traceId, violations(exception));

    LOG.warnf(
        "event=api.rejected status=%d errorCode=%s traceId=%s instance=%s detail=%s",
        problem.status(), problem.errorCode(), traceId, instance, problem.detail());

    return Response.status(problem.status()).type(PROBLEM_JSON).entity(problem).build();
  }

  private Map<String, String> violations(ConstraintViolationException exception) {
    Map<String, String> result = new LinkedHashMap<>();

    exception.getConstraintViolations().stream()
        .sorted((left, right) -> field(left).compareTo(field(right)))
        .forEach(
            violation -> {
              String message = normalizeMessage(violation.getMessage());
              result.put(field(violation), message);
            });

    return result;
  }

  private String normalizeMessage(String message) {
    if (message == null) {
      return null;
    }

    return message
        .replace("igual à", "igual a")
        .replace("maior ou igual à", "maior ou igual a")
        .replace("menor ou igual à", "menor ou igual a");
  }

  private String field(ConstraintViolation<?> violation) {
    String path = violation.getPropertyPath().toString();
    int index = path.lastIndexOf('.');

    return index >= 0 ? path.substring(index + 1) : path;
  }

  private String requestPath() {
    return uriInfo == null ? "" : uriInfo.getRequestUri().getPath();
  }

  private String traceId() {
    Object correlationId = MDC.get("correlationId");

    return correlationId == null ? UUID.randomUUID().toString() : correlationId.toString();
  }
}
