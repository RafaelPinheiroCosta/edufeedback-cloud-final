package br.com.edufeedback.report.service;

import br.com.edufeedback.domain.*;
import br.com.edufeedback.email.EmailSender;
import br.com.edufeedback.persistence.entity.AvaliacaoEntity;
import br.com.edufeedback.persistence.entity.RelatorioSemanalEntity;
import br.com.edufeedback.persistence.repository.*;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WeeklyReportService {
  private static final Logger LOG = Logger.getLogger(WeeklyReportService.class);
  @Inject AvaliacaoRepository avaliacoes;
  @Inject RelatorioSemanalRepository relatorios;
  @Inject EmailSender emailSender;
  @Inject MeterRegistry metrics;

  @ConfigProperty(name = "app.admin-email")
  String adminEmail;

  @ConfigProperty(name = "app.timezone")
  String timezone;

  public LocalDate hoje() {
    return LocalDate.now(ZoneId.of(timezone));
  }

  @Transactional
  public Result gerar(LocalDate referencia) {
    long startedAt = System.nanoTime();
    LOG.infof("event=report.generation.started referenceDate=%s timezone=%s", referencia, timezone);
    LocalDate inicio = referencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate fim = inicio.plusDays(6);
    var existente = relatorios.buscarPeriodo(inicio, fim);
    if (existente.isPresent() && existente.get().status == StatusRelatorio.ENVIADO) {
      LOG.infof(
          "event=report.generation.skipped reason=already-sent reportId=%s periodStart=%s periodEnd=%s",
          existente.get().id, inicio, fim);
      return new Result("JA_ENVIADO", existente.get().id, inicio, fim);
    }
    ZoneId zone = ZoneId.of(timezone);
    Instant inicioInstant = inicio.atStartOfDay(zone).toInstant();
    Instant fimExclusivo = fim.plusDays(1).atStartOfDay(zone).toInstant();
    var itens = avaliacoes.buscarPeriodo(inicioInstant, fimExclusivo);
    int total = itens.size();
    long criticas = itens.stream().filter(a -> a.urgencia == Urgencia.CRITICA).count();
    long atencao = itens.stream().filter(a -> a.urgencia == Urgencia.ATENCAO).count();
    long normais = itens.stream().filter(a -> a.urgencia == Urgencia.NORMAL).count();
    BigDecimal media =
        total == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(itens.stream().mapToInt(a -> a.nota).average().orElse(0))
                .setScale(2, RoundingMode.HALF_UP);
    var r = existente.orElseGet(RelatorioSemanalEntity::new);
    if (r.id == null) r.id = UUID.randomUUID();
    r.dataInicio = inicio;
    r.dataFim = fim;
    r.mediaNotas = media;
    r.totalAvaliacoes = total;
    r.totalCriticas = (int) criticas;
    r.totalAtencao = (int) atencao;
    r.totalNormais = (int) normais;
    r.status = StatusRelatorio.GERADO;
    r.dataGeracao = Instant.now();
    r.conteudo = html(r, itens, zone);
    if (existente.isEmpty()) relatorios.persist(r);
    try {
      emailSender.sendHtml(adminEmail, "Relatório semanal EduFeedback", r.conteudo);
      r.status = StatusRelatorio.ENVIADO;
      r.dataEnvio = Instant.now();
      r.ultimoErro = null;
      metrics.counter("report.sent.total").increment();
      LOG.infof(
          "event=report.sent reportId=%s totalFeedbacks=%d criticalFeedbacks=%d durationMs=%d periodStart=%s periodEnd=%s",
          r.id, total, criticas, elapsedMillis(startedAt), inicio, fim);
      return new Result("ENVIADO", r.id, inicio, fim);
    } catch (RuntimeException e) {
      r.status = StatusRelatorio.FALHOU;
      r.ultimoErro = e.getClass().getSimpleName();
      metrics.counter("report.failed.total").increment();
      LOG.errorf(
          e,
          "event=report.failed reportId=%s durationMs=%d periodStart=%s periodEnd=%s",
          r.id,
          elapsedMillis(startedAt),
          inicio,
          fim);
      throw e;
    }
  }

  private String html(RelatorioSemanalEntity r, List<AvaliacaoEntity> itens, ZoneId zone) {
    var porDia =
        itens.stream()
            .collect(
                Collectors.groupingBy(
                    item -> item.dataEnvio.atZone(zone).toLocalDate(),
                    TreeMap::new,
                    Collectors.counting()));

    var html = new StringBuilder();
    html.append("<h1>Relatório semanal EduFeedback</h1>")
        .append("<p><strong>Período:</strong> ")
        .append(r.dataInicio)
        .append(" a ")
        .append(r.dataFim)
        .append("</p>")
        .append("<p><strong>Total:</strong> ")
        .append(r.totalAvaliacoes)
        .append(" | <strong>Média:</strong> ")
        .append(r.mediaNotas)
        .append("</p>")
        .append("<ul><li>Críticas: ")
        .append(r.totalCriticas)
        .append("</li><li>Atenção: ")
        .append(r.totalAtencao)
        .append("</li><li>Normais: ")
        .append(r.totalNormais)
        .append("</li></ul>")
        .append("<h2>Avaliações por dia</h2><table border=\"1\" cellpadding=\"6\">")
        .append("<tr><th>Data</th><th>Quantidade</th></tr>");

    if (porDia.isEmpty()) {
      html.append("<tr><td colspan=\"2\">Nenhuma avaliação no período.</td></tr>");
    } else {
      porDia.forEach(
          (data, quantidade) ->
              html.append("<tr><td>")
                  .append(data)
                  .append("</td><td>")
                  .append(quantidade)
                  .append("</td></tr>"));
    }
    html.append("</table>")
        .append("<h2>Detalhamento</h2><table border=\"1\" cellpadding=\"6\">")
        .append("<tr><th>Data de envio</th><th>Nota</th><th>Urgência</th><th>Descrição</th></tr>");

    if (itens.isEmpty()) {
      html.append("<tr><td colspan=\"4\">Nenhuma avaliação no período.</td></tr>");
    } else {
      itens.forEach(
          item ->
              html.append("<tr><td>")
                  .append(item.dataEnvio.atZone(zone))
                  .append("</td><td>")
                  .append(item.nota)
                  .append("</td><td>")
                  .append(item.urgencia)
                  .append("</td><td>")
                  .append(escapar(item.descricao))
                  .append("</td></tr>"));
    }
    return html.append("</table>").toString();
  }

  private String escapar(String value) {
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private long elapsedMillis(long startedAt) {
    return (System.nanoTime() - startedAt) / 1_000_000L;
  }

  public record Result(String status, UUID reportId, LocalDate inicio, LocalDate fim) {}
}
