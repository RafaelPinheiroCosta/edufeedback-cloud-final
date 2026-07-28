package br.com.edufeedback.http.auth;

import br.com.edufeedback.http.dto.LoginRequest;
import br.com.edufeedback.http.dto.TokenResponse;
import br.com.edufeedback.persistence.repository.UsuarioRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuthService {
  private static final Logger LOG = Logger.getLogger(AuthService.class);

  @Inject UsuarioRepository usuarioRepository;

  @ConfigProperty(name = "app.security.token-lifespan")
  long tokenLifespan;

  public TokenResponse login(LoginRequest request) {
    var usuario =
        usuarioRepository
            .buscarAtivoPorUsername(request.username())
            .orElseThrow(
                () -> {
                  LOG.warnf(
                      "event=auth.login.failed username=%s reason=invalid_credentials",
                      request.username());
                  return new InvalidCredentialsException();
                });

    if (!BcryptUtil.matches(request.password(), usuario.passwordHash)) {
      LOG.warnf(
          "event=auth.login.failed username=%s reason=invalid_credentials", request.username());
      throw new InvalidCredentialsException();
    }

    String token =
        Jwt.claims()
            .subject(usuario.username)
            .claim("upn", usuario.username)
            .claim("groups", Set.of(usuario.role))
            .claim("preferred_username", usuario.username)
            .sign();

    LOG.infof("event=auth.login.succeeded username=%s role=%s", usuario.username, usuario.role);
    return new TokenResponse(token, "Bearer", tokenLifespan);
  }
}
