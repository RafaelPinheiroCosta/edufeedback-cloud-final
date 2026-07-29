package br.com.edufeedback.notification.diagnostic;

import br.com.edufeedback.domain.CalculadoraUrgencia;
import br.com.edufeedback.domain.Urgencia;
import br.com.edufeedback.persistence.entity.AvaliacaoEntity;
import br.com.edufeedback.persistence.repository.AvaliacaoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class DiagnosticFeedbackPersistence {
  @Inject AvaliacaoRepository repository;

  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public CreatedFeedback create(String descricao, int nota, UUID correlationId) {
    Urgencia urgencia = CalculadoraUrgencia.classificar(nota);
    if (urgencia != Urgencia.CRITICA) {
      throw new IllegalArgumentException("A nota do teste crítico deve estar entre 0 e 4.");
    }

    String normalized =
        descricao == null || descricao.isBlank()
            ? "Reclamação crítica simulada pelo endpoint de diagnóstico."
            : descricao.trim();
    if (normalized.length() < 10 || normalized.length() > 2000) {
      throw new IllegalArgumentException("A descrição deve possuir entre 10 e 2000 caracteres.");
    }

    var entity = new AvaliacaoEntity();
    entity.id = UUID.randomUUID();
    entity.descricao = normalized;
    entity.nota = (short) nota;
    entity.urgencia = urgencia;
    entity.dataEnvio = Instant.now();
    entity.correlationId = correlationId;
    repository.persist(entity);
    repository.flush();

    return new CreatedFeedback(entity.id, entity.nota, entity.urgencia.name());
  }

  public record CreatedFeedback(UUID feedbackId, short nota, String urgencia) {}
}
