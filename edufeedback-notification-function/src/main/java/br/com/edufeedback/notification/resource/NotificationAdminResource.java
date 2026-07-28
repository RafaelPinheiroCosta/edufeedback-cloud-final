package br.com.edufeedback.notification.resource;

import br.com.edufeedback.api.problem.ApiProblem;
import br.com.edufeedback.messaging.FeedbackCriticoEvent;
import br.com.edufeedback.messaging.QueueReceiver;
import br.com.edufeedback.notification.dto.NotificationResponse;
import br.com.edufeedback.notification.dto.NotificationTestRequest;
import br.com.edufeedback.notification.mapper.NotificationMapper;
import br.com.edufeedback.notification.service.NotificationService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/admin/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
@SecurityRequirement(name = "bearerAuth")
@Tag(
    name = "Administração de notificações",
    description = "Endpoints protegidos para demonstração e testes controlados")
public class NotificationAdminResource {
  @Inject QueueReceiver receiver;
  @Inject NotificationService service;
  @Inject NotificationMapper mapper;

  @POST
  @Path("/process-one")
  @Operation(
      summary = "Processar uma mensagem da fila",
      description =
          "Consome manualmente uma mensagem para demonstração; o Queue Trigger continua sendo o fluxo normal.")
  @APIResponses({
    @APIResponse(responseCode = "200", description = "Mensagem processada"),
    @APIResponse(
        responseCode = "401",
        description = "Não autenticado",
        content = @Content(schema = @Schema(implementation = ApiProblem.class)))
  })
  public NotificationResponse processOne() {
    var received = receiver.receiveOne().orElse(null);
    if (received == null) return new NotificationResponse("FILA_VAZIA", null);
    var result = service.processar(received.event());
    received.ack().run();
    return mapper.toResponse(result);
  }

  @POST
  @Path("/test")
  @Operation(
      summary = "Testar notificação por feedback",
      description = "Executa o mesmo serviço usado pelo Queue Trigger para um feedback existente.")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        description = "Notificação processada",
        content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
    @APIResponse(
        responseCode = "400",
        description = "Payload inválido",
        content = @Content(schema = @Schema(implementation = ApiProblem.class))),
    @APIResponse(
        responseCode = "404",
        description = "Feedback inexistente",
        content = @Content(schema = @Schema(implementation = ApiProblem.class)))
  })
  public NotificationResponse test(@Valid NotificationTestRequest request) {
    var event =
        new FeedbackCriticoEvent(
            UUID.randomUUID(),
            "FEEDBACK_CRITICAL_MANUAL_TEST",
            UUID.randomUUID(),
            null,
            Instant.now(),
            new FeedbackCriticoEvent.Payload(request.feedbackId(), (short) 0, "CRITICA"));
    return mapper.toResponse(service.processar(event));
  }
}
