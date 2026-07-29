package br.com.edufeedback.notification.service;

import br.com.edufeedback.api.exception.ResourceNotFoundException;
import br.com.edufeedback.domain.StatusNotificacao;
import br.com.edufeedback.domain.TipoNotificacao;
import br.com.edufeedback.messaging.FeedbackCriticoEvent;
import br.com.edufeedback.persistence.entity.AvaliacaoEntity;
import br.com.edufeedback.persistence.entity.EventoProcessadoEntity;
import br.com.edufeedback.persistence.entity.NotificacaoEntity;
import br.com.edufeedback.persistence.repository.AvaliacaoRepository;
import br.com.edufeedback.persistence.repository.EventoProcessadoRepository;
import br.com.edufeedback.persistence.repository.NotificacaoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class NotificationPersistenceService {
  private static final String CONSUMER = "notification-function";

  @Inject AvaliacaoRepository avaliacoes;
  @Inject NotificacaoRepository notificacoes;
  @Inject EventoProcessadoRepository eventos;

  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public Preparation preparar(FeedbackCriticoEvent event, String adminEmail) {
    if (eventos.jaProcessado(event.eventId(), CONSUMER)) {
      return Preparation.duplicate(event.eventId());
    }

    AvaliacaoEntity avaliacao =
        avaliacoes
            .findByIdOptional(event.payload().feedbackId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "FEEDBACK_NOT_FOUND",
                        "A avaliação " + event.payload().feedbackId() + " não foi encontrada."));

    NotificacaoEntity notificacao =
        notificacoes
            .buscarPorEvento(event.eventId())
            .orElseGet(() -> novaNotificacao(event, avaliacao, adminEmail));

    if (notificacao.status == StatusNotificacao.ENVIADA) {
      return Preparation.alreadySent(event.eventId(), notificacao.id);
    }

    notificacao.status = StatusNotificacao.ENVIANDO;
    notificacao.tentativas++;
    notificacao.ultimoErro = null;
    notificacoes.flush();

    return Preparation.ready(
        event.eventId(),
        notificacao.id,
        notificacao.destinatario,
        notificacao.mensagem,
        notificacao.tentativas);
  }

  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void marcarEnviada(UUID notificacaoId, FeedbackCriticoEvent event) {
    NotificacaoEntity notificacao = buscarNotificacao(notificacaoId);
    notificacao.status = StatusNotificacao.ENVIADA;
    notificacao.dataEnvio = Instant.now();
    notificacao.ultimoErro = null;

    if (!eventos.jaProcessado(event.eventId(), CONSUMER)) {
      var processado = new EventoProcessadoEntity();
      processado.id = UUID.randomUUID();
      processado.eventId = event.eventId();
      processado.consumer = CONSUMER;
      processado.eventType = event.eventType();
      processado.status = "PROCESSADO";
      processado.processedAt = Instant.now();
      processado.correlationId = event.correlationId();
      eventos.persist(processado);
    }

    notificacoes.flush();
    eventos.flush();
  }

  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void marcarFalha(UUID notificacaoId, RuntimeException falha) {
    NotificacaoEntity notificacao = buscarNotificacao(notificacaoId);
    notificacao.status = StatusNotificacao.FALHOU;
    notificacao.ultimoErro = descricaoFalha(falha);
    notificacoes.flush();
  }

  private NotificacaoEntity novaNotificacao(
      FeedbackCriticoEvent event, AvaliacaoEntity avaliacao, String adminEmail) {
    var notificacao = new NotificacaoEntity();
    notificacao.id = UUID.randomUUID();
    notificacao.avaliacao = avaliacao;
    notificacao.eventId = event.eventId();
    notificacao.tipo = TipoNotificacao.EMAIL_CRITICO;
    notificacao.status = StatusNotificacao.PENDENTE;
    notificacao.destinatario = adminEmail;
    notificacao.tentativas = 0;
    notificacao.mensagem =
        "<h1>Feedback crítico</h1><p>Nota: "
            + avaliacao.nota
            + "</p><p>"
            + escapar(avaliacao.descricao)
            + "</p>";
    notificacoes.persist(notificacao);
    return notificacao;
  }

  private NotificacaoEntity buscarNotificacao(UUID notificacaoId) {
    return notificacoes
        .findByIdOptional(notificacaoId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "NOTIFICATION_NOT_FOUND",
                    "A notificação " + notificacaoId + " não foi encontrada."));
  }

  private String descricaoFalha(RuntimeException falha) {
    String mensagem = falha.getMessage();
    String valor =
        falha.getClass().getSimpleName()
            + (mensagem == null || mensagem.isBlank() ? "" : ": " + mensagem);
    return valor.length() <= 4000 ? valor : valor.substring(0, 4000);
  }

  private String escapar(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  public record Preparation(
      String status,
      UUID eventId,
      UUID notificationId,
      String recipient,
      String message,
      int attempt) {

    static Preparation duplicate(UUID eventId) {
      return new Preparation("DUPLICADO", eventId, null, null, null, 0);
    }

    static Preparation alreadySent(UUID eventId, UUID notificationId) {
      return new Preparation("JA_ENVIADA", eventId, notificationId, null, null, 0);
    }

    static Preparation ready(
        UUID eventId, UUID notificationId, String recipient, String message, int attempt) {
      return new Preparation("PRONTA", eventId, notificationId, recipient, message, attempt);
    }

    public boolean shouldSend() {
      return "PRONTA".equals(status);
    }
  }
}
