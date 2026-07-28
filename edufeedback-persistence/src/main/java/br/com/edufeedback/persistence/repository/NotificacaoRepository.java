package br.com.edufeedback.persistence.repository;

import br.com.edufeedback.persistence.entity.NotificacaoEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;

@ApplicationScoped
public class NotificacaoRepository implements PanacheRepositoryBase<NotificacaoEntity, UUID> {
  public Optional<NotificacaoEntity> buscarPorEvento(UUID eventId) {
    return find("eventId", eventId).firstResultOptional();
  }
}
