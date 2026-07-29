package br.com.edufeedback.http.service;

import br.com.edufeedback.domain.CalculadoraUrgencia;
import br.com.edufeedback.http.dto.CriarAvaliacaoRequest;
import br.com.edufeedback.persistence.entity.AvaliacaoEntity;
import br.com.edufeedback.persistence.repository.AvaliacaoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class AvaliacaoPersistenceService {

  @Inject AvaliacaoRepository repository;

  @Transactional
  public AvaliacaoEntity salvar(CriarAvaliacaoRequest request, UUID correlationId) {
    var entity = new AvaliacaoEntity();
    entity.id = UUID.randomUUID();
    entity.descricao = request.descricao().trim();
    entity.nota = request.nota().shortValue();
    entity.urgencia = CalculadoraUrgencia.classificar(request.nota());
    entity.dataEnvio = Instant.now();
    entity.correlationId = correlationId;
    repository.persist(entity);
    repository.flush();
    return entity;
  }
}
