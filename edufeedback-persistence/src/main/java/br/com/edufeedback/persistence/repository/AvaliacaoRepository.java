package br.com.edufeedback.persistence.repository;

import br.com.edufeedback.persistence.entity.AvaliacaoEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class AvaliacaoRepository implements PanacheRepositoryBase<AvaliacaoEntity, UUID> {
  public List<AvaliacaoEntity> buscarPeriodo(Instant inicio, Instant fim) {
    return list("dataEnvio >= ?1 and dataEnvio < ?2 order by dataEnvio", inicio, fim);
  }
}
