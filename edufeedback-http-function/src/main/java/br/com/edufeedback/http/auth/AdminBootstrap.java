package br.com.edufeedback.http.auth;

import br.com.edufeedback.persistence.entity.UsuarioEntity;
import br.com.edufeedback.persistence.repository.UsuarioRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AdminBootstrap {
  private static final Logger LOG = Logger.getLogger(AdminBootstrap.class);
  private static final String USERNAME = "admin";
  private static final String DEMO_PASSWORD = "admin123";

  @Inject UsuarioRepository usuarioRepository;

  @Transactional
  void onStart(@Observes StartupEvent event) {
    if (usuarioRepository.find("username", USERNAME).count() > 0) {
      LOG.infof("event=security.bootstrap.skipped username=%s reason=already_exists", USERNAME);
      return;
    }

    var admin = new UsuarioEntity();
    admin.id = UUID.randomUUID();
    admin.username = USERNAME;
    admin.passwordHash = BcryptUtil.bcryptHash(DEMO_PASSWORD);
    admin.role = "ADMIN";
    admin.ativo = true;
    usuarioRepository.persist(admin);

    LOG.infof("event=security.bootstrap.created username=%s role=%s", admin.username, admin.role);
  }
}
