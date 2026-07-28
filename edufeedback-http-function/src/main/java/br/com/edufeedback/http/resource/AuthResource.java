package br.com.edufeedback.http.resource;

import br.com.edufeedback.api.problem.ApiProblem;
import br.com.edufeedback.http.auth.AuthService;
import br.com.edufeedback.http.dto.LoginRequest;
import br.com.edufeedback.http.dto.TokenResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
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
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Autenticação", description = "Autenticação do administrador de demonstração")
public class AuthResource {
  @Inject AuthService authService;

  @POST
  @Path("/login")
  @PermitAll
  @Operation(
      summary = "Autenticar administrador",
      description = "Emite JWT para o administrador bootstrap da demonstração.")
  @APIResponses({
    @APIResponse(
        responseCode = "200",
        description = "Token emitido",
        content = @Content(schema = @Schema(implementation = TokenResponse.class))),
    @APIResponse(
        responseCode = "400",
        description = "Requisição inválida",
        content = @Content(schema = @Schema(implementation = ApiProblem.class))),
    @APIResponse(
        responseCode = "401",
        description = "Credenciais inválidas",
        content = @Content(schema = @Schema(implementation = ApiProblem.class)))
  })
  public TokenResponse login(@Valid LoginRequest request) {
    return authService.login(request);
  }
}
