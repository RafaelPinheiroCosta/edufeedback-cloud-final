package br.com.edufeedback.http.service;

import br.com.edufeedback.api.exception.ResourceNotFoundException;
import br.com.edufeedback.domain.*;
import br.com.edufeedback.http.dto.*;
import br.com.edufeedback.http.mapper.AvaliacaoMapper;
import br.com.edufeedback.messaging.*;
import br.com.edufeedback.persistence.entity.AvaliacaoEntity;
import br.com.edufeedback.persistence.repository.AvaliacaoRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.*;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AvaliacaoService {
  private static final Logger LOG = Logger.getLogger(AvaliacaoService.class);
  @Inject AvaliacaoRepository repository;
  @Inject QueuePublisher publisher;
  @Inject MeterRegistry metrics;
  @Inject AvaliacaoMapper mapper;

  @Transactional
  public AvaliacaoResponse criar(CriarAvaliacaoRequest request, UUID correlationId) {
    var entity = new AvaliacaoEntity();
    entity.id = UUID.randomUUID();
    entity.descricao = request.descricao().trim();
    entity.nota = request.nota().shortValue();
    entity.urgencia = CalculadoraUrgencia.classificar(request.nota());
    entity.dataEnvio = Instant.now();
    entity.correlationId = correlationId;
    repository.persist(entity);
    repository.flush();

    LOG.infof(
        "event=feedback.created feedbackId=%s urgency=%s correlationId=%s",
        entity.id, entity.urgencia, correlationId);
    metrics.counter("feedback.received.total", "urgencia", entity.urgencia.name()).increment();
    if (entity.urgencia == Urgencia.CRITICA) {
      var event =
          new FeedbackCriticoEvent(
              UUID.randomUUID(),
              "FEEDBACK_CRITICAL_CREATED",
              correlationId,
              null,
              Instant.now(),
              new FeedbackCriticoEvent.Payload(entity.id, entity.nota, entity.urgencia.name()));
      publisher.publish(event);
      LOG.infof(
          "event=queue.message.published eventId=%s feedbackId=%s correlationId=%s",
          event.eventId(), entity.id, correlationId);
      metrics.counter("queue.messages.published").increment();
    }
    return mapper.toResponse(entity);
  }

  public List<AvaliacaoResponse> listar() {
    return repository.listAll().stream().map(mapper::toResponse).toList();
  }

  public AvaliacaoResponse buscar(UUID id) {
    return repository
        .findByIdOptional(id)
        .map(mapper::toResponse)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "FEEDBACK_NOT_FOUND", "A avaliação " + id + " não foi encontrada."));
  }
}
