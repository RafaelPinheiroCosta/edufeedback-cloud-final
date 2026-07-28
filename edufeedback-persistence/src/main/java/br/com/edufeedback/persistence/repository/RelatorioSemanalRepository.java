package br.com.edufeedback.persistence.repository;

import br.com.edufeedback.persistence.entity.RelatorioSemanalEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.*;

@ApplicationScoped
public class RelatorioSemanalRepository
    implements PanacheRepositoryBase<RelatorioSemanalEntity, UUID> {
  public Optional<RelatorioSemanalEntity> buscarPeriodo(LocalDate inicio, LocalDate fim) {
    return find("dataInicio = ?1 and dataFim = ?2", inicio, fim).firstResultOptional();
  }
}
