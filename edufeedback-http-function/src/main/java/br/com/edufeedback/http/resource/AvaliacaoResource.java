package br.com.edufeedback.http.resource;

import br.com.edufeedback.api.observability.CorrelationIdFilter;
import br.com.edufeedback.api.problem.ApiProblem;
import br.com.edufeedback.http.dto.AvaliacaoResponse;
import br.com.edufeedback.http.dto.CriarAvaliacaoRequest;
import br.com.edufeedback.http.service.AvaliacaoService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/feedbacks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Feedbacks", description = "Recebimento e consulta administrativa de feedbacks")
public class AvaliacaoResource {
  @Inject AvaliacaoService service;

  @POST
  @PermitAll
  @Operation(
      summary = "Enviar feedback",
      description = "Endpoint público para envio de feedback estudantil.")
  @APIResponses({
    @APIResponse(
        responseCode = "201",
        description = "Feedback criado",
        content = @Content(schema = @Schema(implementation = AvaliacaoResponse.class))),
    @APIResponse(
        responseCode = "400",
        description = "Requisição inválida (JSON, cabeçalhos ou validação)",
        content = @Content(schema = @Schema(implementation = ApiProblem.class)))
  })
  public Response criar(@Valid CriarAvaliacaoRequest request) {
    if (request == null) {
      throw new BadRequestException("O corpo JSON da requisição é obrigatório.");
    }
    UUID correlationId = UUID.fromString(CorrelationIdFilter.current());
    var response = service.criar(request, correlationId);
    return Response.created(URI.create("/api/v1/feedbacks/" + response.id()))
        .entity(response)
        .header("X-Correlation-ID", correlationId)
        .build();
  }

  @GET
  @RolesAllowed("ADMIN")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(
      summary = "Listar feedbacks",
      description = "Consulta administrativa protegida por JWT.")
  @APIResponses({
    @APIResponse(responseCode = "200", description = "Lista retornada"),
    @APIResponse(
        responseCode = "401",
        description = "Token ausente ou inválido",
        content = @Content(schema = @Schema(implementation = ApiProblem.class))),
    @APIResponse(
        responseCode = "403",
        description = "Perfil sem permissão",
        content = @Content(schema = @Schema(implementation = ApiProblem.class)))
  })
  public List<AvaliacaoResponse> listar() {
    return service.listar();
  }

  @GET
  @Path("/{id}")
  @RolesAllowed("ADMIN")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Buscar feedback por ID")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        description = "Feedback encontrado",
        content = @Content(schema = @Schema(implementation = AvaliacaoResponse.class))),
    @APIResponse(
        responseCode = "404",
        description = "Feedback não encontrado",
        content = @Content(schema = @Schema(implementation = ApiProblem.class)))
  })
  public AvaliacaoResponse buscar(@PathParam("id") UUID id) {
    return service.buscar(id);
  }
}
