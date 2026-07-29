package br.com.edufeedback.report.service;

import br.com.edufeedback.email.EmailSender;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WeeklyReportService {
  private static final Logger LOG = Logger.getLogger(WeeklyReportService.class);

  @Inject WeeklyReportPersistenceService persistence;
  @Inject EmailSender emailSender;
  @Inject MeterRegistry metrics;

  @ConfigProperty(name = "app.admin-email")
  String adminEmail;

  @ConfigProperty(name = "app.timezone")
  String timezone;

  public LocalDate hoje() {
    return LocalDate.now(ZoneId.of(timezone));
  }

  public Result gerar(LocalDate referencia) {
    long startedAt = System.nanoTime();
    ZoneId zone = ZoneId.of(timezone);
    LOG.infof("event=report.generation.started referenceDate=%s timezone=%s", referencia, timezone);

    WeeklyReportPersistenceService.Preparation preparation = persistence.preparar(referencia, zone);

    if (!preparation.shouldSend()) {
      LOG.infof(
          "event=report.generation.skipped reason=already-sent reportId=%s periodStart=%s periodEnd=%s",
          preparation.reportId(), preparation.start(), preparation.end());
      return new Result(
          preparation.status(), preparation.reportId(), preparation.start(), preparation.end());
    }

    try {
      emailSender.sendHtml(adminEmail, "Relatório semanal EduFeedback", preparation.content());
    } catch (RuntimeException emailFailure) {
      persistFailureWithoutMaskingOriginal(preparation.reportId(), emailFailure);
      metrics.counter("report.failed.total").increment();
      LOG.errorf(
          emailFailure,
          "event=report.failed phase=email reportId=%s durationMs=%d periodStart=%s periodEnd=%s",
          preparation.reportId(),
          elapsedMillis(startedAt),
          preparation.start(),
          preparation.end());
      throw emailFailure;
    }

    try {
      persistence.marcarEnviado(preparation.reportId());
    } catch (RuntimeException persistenceFailure) {
      LOG.errorf(
          persistenceFailure,
          "event=report.failed phase=confirm-after-email reportId=%s durationMs=%d periodStart=%s periodEnd=%s warning=email-may-have-been-delivered",
          preparation.reportId(),
          elapsedMillis(startedAt),
          preparation.start(),
          preparation.end());
      throw persistenceFailure;
    }

    metrics.counter("report.sent.total").increment();
    LOG.infof(
        "event=report.sent reportId=%s totalFeedbacks=%d criticalFeedbacks=%d durationMs=%d periodStart=%s periodEnd=%s",
        preparation.reportId(),
        preparation.totalFeedbacks(),
        preparation.criticalFeedbacks(),
        elapsedMillis(startedAt),
        preparation.start(),
        preparation.end());
    return new Result("ENVIADO", preparation.reportId(), preparation.start(), preparation.end());
  }

  private void persistFailureWithoutMaskingOriginal(
      UUID reportId, RuntimeException originalFailure) {
    try {
      persistence.marcarFalha(reportId, originalFailure);
    } catch (RuntimeException persistenceFailure) {
      originalFailure.addSuppressed(persistenceFailure);
      LOG.errorf(
          persistenceFailure, "event=report.failure-state.persist.failed reportId=%s", reportId);
    }
  }

  private long elapsedMillis(long startedAt) {
    return (System.nanoTime() - startedAt) / 1_000_000L;
  }

  public record Result(String status, UUID reportId, LocalDate inicio, LocalDate fim) {}
}
