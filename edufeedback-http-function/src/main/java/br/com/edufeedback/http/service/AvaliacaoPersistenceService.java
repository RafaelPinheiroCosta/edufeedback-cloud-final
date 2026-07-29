package br.com.edufeedback.http.service;

import br.com.edufeedback.persistence.entity.AvaliacaoEntity;
import br.com.edufeedback.persistence.repository.AvaliacaoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AvaliacaoPersistenceService {
  @Inject AvaliacaoRepository repository;

  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void persistirEConfirmar(AvaliacaoEntity entity) {
    repository.persist(entity);
    repository.flush();
  }
}
