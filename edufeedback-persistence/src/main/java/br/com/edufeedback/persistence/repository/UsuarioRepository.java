package br.com.edufeedback.persistence.repository;

import br.com.edufeedback.persistence.entity.UsuarioEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UsuarioRepository implements PanacheRepositoryBase<UsuarioEntity, UUID> {
  public Optional<UsuarioEntity> buscarAtivoPorUsername(String username) {
    return find("username = ?1 and ativo = true", username).firstResultOptional();
  }
}
