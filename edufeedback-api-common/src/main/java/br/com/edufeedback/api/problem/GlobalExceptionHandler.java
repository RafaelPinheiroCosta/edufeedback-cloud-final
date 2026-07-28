package br.com.edufeedback.api.problem;

import br.com.edufeedback.api.exception.AuthenticationException;
import br.com.edufeedback.api.exception.BusinessException;
import br.com.edufeedback.api.exception.ExternalServiceException;
import br.com.edufeedback.api.exception.InvalidRequestException;
import br.com.edufeedback.api.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import jakarta.annotation.Priority;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

/** Único ponto de tradução de exceções da API para application/problem+json. */
@Provider
@Priority(Priorities.USER)
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {
  private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

  @Context UriInfo uriInfo;

  @Override
  public Response toResponse(Throwable error) {
    String instance = uriInfo == null ? "" : uriInfo.getRequestUri().getPath();
    String traceId = traceId();
    ApiProblem problem = map(error, instance, traceId);

    if (problem.status() >= 500) {
      LOG.errorf(
          error,
          "event=api.error status=%d errorCode=%s traceId=%s instance=%s",
          problem.status(),
          problem.errorCode(),
          traceId,
          instance);
    } else {
      LOG.warnf(
          "event=api.rejected status=%d errorCode=%s traceId=%s instance=%s detail=%s",
          problem.status(), problem.errorCode(), traceId, instance, problem.detail());
    }

    return Response.status(problem.status())
        .type("application/problem+json")
        .entity(problem)
        .build();
  }

  private ApiProblem map(Throwable error, String instance, String traceId) {
    if (error instanceof ConstraintViolationException validation) {
      return ApiProblem.validation(
          "A requisição contém campos inválidos.", instance, traceId, violations(validation));
    }
    if (hasCause(error, JsonProcessingException.class)) {
      return ApiProblem.of(
          400,
          "JSON inválido",
          "O corpo da requisição contém JSON vazio, incompleto ou malformado.",
          instance,
          "MALFORMED_JSON",
          traceId);
    }
    if (error instanceof AuthenticationException authentication) {
      return ApiProblem.of(
          401,
          "Credenciais inválidas",
          authentication.getMessage(),
          instance,
          authentication.errorCode(),
          traceId);
    }
    if (error instanceof InvalidRequestException invalidRequest) {
      return ApiProblem.of(
          400,
          "Requisição inválida",
          invalidRequest.getMessage(),
          instance,
          invalidRequest.errorCode(),
          traceId);
    }
    if (error instanceof ResourceNotFoundException notFound) {
      return ApiProblem.of(
          404,
          "Recurso não encontrado",
          notFound.getMessage(),
          instance,
          notFound.errorCode(),
          traceId);
    }
    if (error instanceof BusinessException business) {
      return ApiProblem.of(
          422,
          "Regra de negócio violada",
          business.getMessage(),
          instance,
          business.errorCode(),
          traceId);
    }
    if (error instanceof ExternalServiceException external) {
      return ApiProblem.of(
          502,
          "Falha em serviço externo",
          "Uma integração externa não concluiu a operação.",
          instance,
          external.errorCode(),
          traceId);
    }
    if (error instanceof UnauthorizedException || error instanceof NotAuthorizedException) {
      return ApiProblem.of(
          401,
          "Não autenticado",
          "Envie um Bearer Token JWT válido.",
          instance,
          "UNAUTHORIZED",
          traceId);
    }
    if (error instanceof ForbiddenException || error instanceof jakarta.ws.rs.ForbiddenException) {
      return ApiProblem.of(
          403,
          "Acesso negado",
          "O usuário autenticado não possui permissão para este recurso.",
          instance,
          "FORBIDDEN",
          traceId);
    }
    if (error instanceof NotAllowedException) {
      return ApiProblem.of(
          405,
          "Método não permitido",
          "O método HTTP não é aceito para este recurso.",
          instance,
          "METHOD_NOT_ALLOWED",
          traceId);
    }
    if (error instanceof NotSupportedException) {
      return ApiProblem.of(
          415,
          "Tipo de mídia não suportado",
          "Utilize Content-Type application/json quando houver corpo na requisição.",
          instance,
          "UNSUPPORTED_MEDIA_TYPE",
          traceId);
    }
    if (error instanceof DateTimeParseException || error instanceof IllegalArgumentException) {
      return ApiProblem.of(
          400,
          "Requisição inválida",
          safeMessage(error, "Um parâmetro possui formato inválido."),
          instance,
          "INVALID_REQUEST",
          traceId);
    }
    if (error instanceof BadRequestException) {
      return ApiProblem.of(
          400,
          "Requisição inválida",
          safeMessage(error, "A requisição não pôde ser processada."),
          instance,
          "BAD_REQUEST",
          traceId);
    }
    if (error instanceof WebApplicationException web) {
      int status = web.getResponse().getStatus();
      return ApiProblem.of(
          status,
          "Erro HTTP",
          safeMessage(error, "A requisição não pôde ser processada."),
          instance,
          "HTTP_" + status,
          traceId);
    }
    return ApiProblem.of(
        500,
        "Erro interno",
        "Ocorreu um erro inesperado. Consulte os logs pelo traceId.",
        instance,
        "INTERNAL_ERROR",
        traceId);
  }

  private Map<String, String> violations(ConstraintViolationException exception) {
    Map<String, String> result = new LinkedHashMap<>();
    exception.getConstraintViolations().stream()
        .sorted((left, right) -> field(left).compareTo(field(right)))
        .forEach(violation -> result.put(field(violation), violation.getMessage()));
    return result;
  }

  private String field(ConstraintViolation<?> violation) {
    String path = violation.getPropertyPath().toString();
    int index = path.lastIndexOf('.');
    return index >= 0 ? path.substring(index + 1) : path;
  }

  private String traceId() {
    Object correlationId = MDC.get("correlationId");
    return correlationId == null ? UUID.randomUUID().toString() : correlationId.toString();
  }

  private boolean hasCause(Throwable error, Class<? extends Throwable> type) {
    Throwable current = error;
    while (current != null) {
      if (type.isInstance(current)) {
        return true;
      }
      if (current == current.getCause()) {
        break;
      }
      current = current.getCause();
    }
    return false;
  }

  private String safeMessage(Throwable error, String fallback) {
    return error.getMessage() == null || error.getMessage().isBlank()
        ? fallback
        : error.getMessage();
  }
}
