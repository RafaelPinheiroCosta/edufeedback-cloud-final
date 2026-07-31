package br.com.edufeedback.api.observability;

import br.com.edufeedback.api.exception.InvalidRequestException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

@Provider
@Priority(Priorities.AUTHENTICATION - 10)
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {
  public static final String HEADER = "X-Correlation-ID";
  private static final String START_NANOS = CorrelationIdFilter.class.getName() + ".startNanos";
  private static final Logger LOG = Logger.getLogger(CorrelationIdFilter.class);

  @Override
  public void filter(ContainerRequestContext request) {
    String correlationId = resolve(request.getHeaderString(HEADER));
    request.setProperty(HEADER, correlationId);
    request.setProperty(START_NANOS, System.nanoTime());
    MDC.put("correlationId", correlationId);
    MDC.put("httpMethod", request.getMethod());
    MDC.put("httpPath", request.getUriInfo().getPath());
    LOG.infof(
        "event=http.request.started method=%s path=%s correlationId=%s",
        request.getMethod(), request.getUriInfo().getPath(), correlationId);
  }

  @Override
  public void filter(ContainerRequestContext request, ContainerResponseContext response) {
    String correlationId = String.valueOf(request.getProperty(HEADER));
    response.getHeaders().putSingle(HEADER, correlationId);
    long durationMs = durationMillis(request.getProperty(START_NANOS));
    LOG.infof(
        "event=http.request.completed method=%s path=%s status=%d durationMs=%d correlationId=%s",
        request.getMethod(),
        request.getUriInfo().getPath(),
        response.getStatus(),
        durationMs,
        correlationId);
    MDC.clear();
  }

  public static String current() {
    Object value = MDC.get("correlationId");
    return value == null ? UUID.randomUUID().toString() : value.toString();
  }

  private String resolve(String value) {
    if (value == null || value.isBlank()) {
      return UUID.randomUUID().toString();
    }
    try {
      return UUID.fromString(value.trim()).toString();
    } catch (IllegalArgumentException exception) {
      throw new InvalidRequestException(
          "INVALID_CORRELATION_ID", "X-Correlation-ID deve ser um UUID válido.");
    }
  }

  private long durationMillis(Object startNanos) {
    if (!(startNanos instanceof Long start)) {
      return 0L;
    }
    return (System.nanoTime() - start) / 1_000_000L;
  }
}
