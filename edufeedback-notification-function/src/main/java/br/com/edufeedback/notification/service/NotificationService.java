package br.com.edufeedback.notification.service;

import br.com.edufeedback.email.EmailSender;
import br.com.edufeedback.messaging.FeedbackCriticoEvent;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NotificationService {
  private static final Logger LOG = Logger.getLogger(NotificationService.class);

  @Inject NotificationPersistenceService persistence;
  @Inject EmailSender emailSender;
  @Inject MeterRegistry metrics;

  @ConfigProperty(name = "app.admin-email")
  String adminEmail;

  public ProcessingResult processar(FeedbackCriticoEvent event) {
    long startedAt = System.nanoTime();
    LOG.infof(
        "event=notification.processing.started eventId=%s feedbackId=%s correlationId=%s",
        event.eventId(), event.payload().feedbackId(), event.correlationId());

    NotificationPersistenceService.Preparation preparation =
        persistence.preparar(event, adminEmail);

    if (!preparation.shouldSend()) {
      LOG.infof(
          "event=notification.processing.skipped status=%s eventId=%s correlationId=%s",
          preparation.status(), event.eventId(), event.correlationId());
      return new ProcessingResult(preparation.status(), event.eventId());
    }

    try {
      emailSender.sendHtml(
          preparation.recipient(), "Feedback crítico recebido", preparation.message());
    } catch (RuntimeException emailFailure) {
      persistFailureWithoutMaskingOriginal(preparation.notificationId(), emailFailure, event);
      metrics.counter("notification.failed.total").increment();
      LOG.errorf(
          emailFailure,
          "event=notification.failed phase=email eventId=%s feedbackId=%s notificationId=%s attempt=%d durationMs=%d correlationId=%s",
          event.eventId(),
          event.payload().feedbackId(),
          preparation.notificationId(),
          preparation.attempt(),
          elapsedMillis(startedAt),
          event.correlationId());
      throw emailFailure;
    }

    try {
      persistence.marcarEnviada(preparation.notificationId(), event);
    } catch (RuntimeException persistenceFailure) {
      LOG.errorf(
          persistenceFailure,
          "event=notification.failed phase=confirm-after-email eventId=%s feedbackId=%s notificationId=%s attempt=%d durationMs=%d correlationId=%s warning=email-may-have-been-delivered",
          event.eventId(),
          event.payload().feedbackId(),
          preparation.notificationId(),
          preparation.attempt(),
          elapsedMillis(startedAt),
          event.correlationId());
      throw persistenceFailure;
    }

    metrics.counter("notification.sent.total").increment();
    LOG.infof(
        "event=notification.sent eventId=%s feedbackId=%s notificationId=%s attempt=%d durationMs=%d correlationId=%s",
        event.eventId(),
        event.payload().feedbackId(),
        preparation.notificationId(),
        preparation.attempt(),
        elapsedMillis(startedAt),
        event.correlationId());
    return new ProcessingResult("ENVIADA", event.eventId());
  }

  private void persistFailureWithoutMaskingOriginal(
      UUID notificationId, RuntimeException originalFailure, FeedbackCriticoEvent event) {
    try {
      persistence.marcarFalha(notificationId, originalFailure);
    } catch (RuntimeException persistenceFailure) {
      originalFailure.addSuppressed(persistenceFailure);
      LOG.errorf(
          persistenceFailure,
          "event=notification.failure-state.persist.failed eventId=%s notificationId=%s correlationId=%s",
          event.eventId(),
          notificationId,
          event.correlationId());
    }
  }

  private long elapsedMillis(long startedAt) {
    return (System.nanoTime() - startedAt) / 1_000_000L;
  }

  public record ProcessingResult(String status, UUID eventId) {}
}
