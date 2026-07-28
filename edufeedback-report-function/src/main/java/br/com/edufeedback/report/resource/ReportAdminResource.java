package br.com.edufeedback.report.resource;

import br.com.edufeedback.api.problem.ApiProblem;
import br.com.edufeedback.report.dto.GenerateReportRequest;
import br.com.edufeedback.report.dto.ReportResponse;
import br.com.edufeedback.report.mapper.ReportMapper;
import br.com.edufeedback.report.service.WeeklyReportService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/admin/reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
@SecurityRequirement(name = "bearerAuth")
@Tag(
    name = "Administração de relatórios",
    description = "Geração manual protegida para demonstração")
public class ReportAdminResource {
  @Inject WeeklyReportService service;
  @Inject ReportMapper mapper;

  @POST
  @Path("/weekly")
  @Operation(
      summary = "Gerar relatório semanal",
      description = "Executa manualmente o mesmo serviço utilizado pelo Timer Trigger.")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        description = "Relatório processado",
        content = @Content(schema = @Schema(implementation = ReportResponse.class))),
    @APIResponse(
        responseCode = "401",
        description = "Não autenticado",
        content = @Content(schema = @Schema(implementation = ApiProblem.class))),
    @APIResponse(
        responseCode = "502",
        description = "Falha no envio de e-mail",
        content = @Content(schema = @Schema(implementation = ApiProblem.class)))
  })
  public ReportResponse gerar(GenerateReportRequest request) {
    var reference =
        request == null || request.referenceDate() == null
            ? service.hoje()
            : request.referenceDate();
    return mapper.toResponse(service.gerar(reference));
  }
}
