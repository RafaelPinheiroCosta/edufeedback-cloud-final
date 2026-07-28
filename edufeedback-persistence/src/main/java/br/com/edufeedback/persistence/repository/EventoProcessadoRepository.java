package br.com.edufeedback.persistence.repository;

import br.com.edufeedback.persistence.entity.EventoProcessadoEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;

@ApplicationScoped
public class EventoProcessadoRepository
    implements PanacheRepositoryBase<EventoProcessadoEntity, UUID> {
  public boolean jaProcessado(UUID eventId, String consumer) {
    return count("eventId = ?1 and consumer = ?2", eventId, consumer) > 0;
  }
}
