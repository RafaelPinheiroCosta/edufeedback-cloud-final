package br.com.edufeedback.notification.service;

import br.com.edufeedback.api.exception.ResourceNotFoundException;
import br.com.edufeedback.domain.*;
import br.com.edufeedback.email.EmailSender;
import br.com.edufeedback.messaging.FeedbackCriticoEvent;
import br.com.edufeedback.persistence.entity.*;
import br.com.edufeedback.persistence.repository.*;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NotificationService {
  private static final Logger LOG = Logger.getLogger(NotificationService.class);
  static final String CONSUMER = "notification-function";
  @Inject AvaliacaoRepository avaliacoes;
  @Inject NotificacaoRepository notificacoes;
  @Inject EventoProcessadoRepository eventos;
  @Inject EmailSender emailSender;
  @Inject MeterRegistry metrics;

  @ConfigProperty(name = "app.admin-email")
  String adminEmail;

  @Transactional
  public ProcessingResult processar(FeedbackCriticoEvent event) {
    long startedAt = System.nanoTime();
    LOG.infof(
        "event=notification.processing.started eventId=%s feedbackId=%s correlationId=%s",
        event.eventId(), event.payload().feedbackId(), event.correlationId());
    if (eventos.jaProcessado(event.eventId(), CONSUMER)) {
      LOG.infof(
          "event=notification.processing.skipped reason=duplicate eventId=%s correlationId=%s",
          event.eventId(), event.correlationId());
      return new ProcessingResult("DUPLICADO", event.eventId());
    }
    var avaliacao =
        avaliacoes
            .findByIdOptional(event.payload().feedbackId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "FEEDBACK_NOT_FOUND",
                        "A avaliação " + event.payload().feedbackId() + " não foi encontrada."));
    var notificacao =
        notificacoes
            .buscarPorEvento(event.eventId())
            .orElseGet(() -> novaNotificacao(event, avaliacao));
    if (notificacao.status == StatusNotificacao.ENVIADA)
      return new ProcessingResult("JA_ENVIADA", event.eventId());
    notificacao.status = StatusNotificacao.ENVIANDO;
    notificacao.tentativas++;
    try {
      emailSender.sendHtml(adminEmail, "Feedback crítico recebido", notificacao.mensagem);
      notificacao.status = StatusNotificacao.ENVIADA;
      notificacao.dataEnvio = Instant.now();
      notificacao.ultimoErro = null;
      var processed = new EventoProcessadoEntity();
      processed.id = UUID.randomUUID();
      processed.eventId = event.eventId();
      processed.consumer = CONSUMER;
      processed.eventType = event.eventType();
      processed.status = "PROCESSADO";
      processed.processedAt = Instant.now();
      processed.correlationId = event.correlationId();
      eventos.persist(processed);
      metrics.counter("notification.sent.total").increment();
      LOG.infof(
          "event=notification.sent eventId=%s feedbackId=%s durationMs=%d correlationId=%s",
          event.eventId(),
          event.payload().feedbackId(),
          elapsedMillis(startedAt),
          event.correlationId());
      return new ProcessingResult("ENVIADA", event.eventId());
    } catch (RuntimeException e) {
      notificacao.status = StatusNotificacao.FALHOU;
      notificacao.ultimoErro = e.getClass().getSimpleName();
      metrics.counter("notification.failed.total").increment();
      LOG.errorf(
          e,
          "event=notification.failed eventId=%s feedbackId=%s durationMs=%d correlationId=%s",
          event.eventId(),
          event.payload().feedbackId(),
          elapsedMillis(startedAt),
          event.correlationId());
      throw e;
    }
  }

  private NotificacaoEntity novaNotificacao(FeedbackCriticoEvent event, AvaliacaoEntity avaliacao) {
    var n = new NotificacaoEntity();
    n.id = UUID.randomUUID();
    n.avaliacao = avaliacao;
    n.eventId = event.eventId();
    n.tipo = TipoNotificacao.EMAIL_CRITICO;
    n.status = StatusNotificacao.PENDENTE;
    n.destinatario = adminEmail;
    n.tentativas = 0;
    n.mensagem =
        "<h1>Feedback crítico</h1><p>Nota: "
            + avaliacao.nota
            + "</p><p>"
            + escapar(avaliacao.descricao)
            + "</p>";
    notificacoes.persist(n);
    return n;
  }

  private String escapar(String value) {
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private long elapsedMillis(long startedAt) {
    return (System.nanoTime() - startedAt) / 1_000_000L;
  }

  public record ProcessingResult(String status, UUID eventId) {}
}
