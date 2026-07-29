package br.com.edufeedback.notification.diagnostic;

import br.com.edufeedback.persistence.repository.AvaliacaoRepository;
import br.com.edufeedback.persistence.repository.EventoProcessadoRepository;
import br.com.edufeedback.persistence.repository.NotificacaoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class NotificationDiagnosticQueryService {
  private static final String CONSUMER = "notification-function";

  @Inject AvaliacaoRepository avaliacoes;
  @Inject NotificacaoRepository notificacoes;
  @Inject EventoProcessadoRepository eventos;

  @Transactional(Transactional.TxType.SUPPORTS)
  public Snapshot consultar(UUID eventId, UUID requestedFeedbackId) {
    var notification = notificacoes.buscarPorEvento(eventId).orElse(null);
    UUID storedFeedbackId =
        notification == null || notification.avaliacao == null ? null : notification.avaliacao.id;
    UUID feedbackId = requestedFeedbackId != null ? requestedFeedbackId : storedFeedbackId;
    boolean feedbackExists =
        feedbackId != null && avaliacoes.findByIdOptional(feedbackId).isPresent();
    boolean processed = eventos.jaProcessado(eventId, CONSUMER);

    if (notification == null) {
      String status =
          feedbackId != null && !feedbackExists
              ? "FEEDBACK_NAO_ENCONTRADO"
              : "AGUARDANDO_PROCESSAMENTO";
      return new Snapshot(
          eventId,
          requestedFeedbackId,
          storedFeedbackId,
          feedbackExists,
          false,
          processed,
          null,
          status,
          0,
          null,
          null,
          null);
    }

    return new Snapshot(
        eventId,
        requestedFeedbackId,
        storedFeedbackId,
        feedbackExists,
        true,
        processed,
        notification.id,
        notification.status.name(),
        notification.tentativas,
        notification.ultimoErro,
        mask(notification.destinatario),
        notification.dataEnvio);
  }

  private String mask(String email) {
    if (email == null || !email.contains("@")) {
      return "***";
    }
    return "***@" + email.substring(email.indexOf('@') + 1);
  }

  public record Snapshot(
      UUID eventId,
      UUID requestedFeedbackId,
      UUID storedFeedbackId,
      boolean feedbackExists,
      boolean notificationFound,
      boolean processedEvent,
      UUID notificationId,
      String status,
      int attempts,
      String lastError,
      String recipient,
      Instant sentAt) {}
}
