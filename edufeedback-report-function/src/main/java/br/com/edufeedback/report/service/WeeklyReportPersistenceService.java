package br.com.edufeedback.report.service;

import br.com.edufeedback.api.exception.ResourceNotFoundException;
import br.com.edufeedback.domain.StatusRelatorio;
import br.com.edufeedback.domain.Urgencia;
import br.com.edufeedback.persistence.entity.AvaliacaoEntity;
import br.com.edufeedback.persistence.entity.RelatorioSemanalEntity;
import br.com.edufeedback.persistence.repository.AvaliacaoRepository;
import br.com.edufeedback.persistence.repository.RelatorioSemanalRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class WeeklyReportPersistenceService {
  @Inject AvaliacaoRepository avaliacoes;
  @Inject RelatorioSemanalRepository relatorios;

  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public Preparation preparar(LocalDate referencia, ZoneId zone) {
    LocalDate inicio = referencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate fim = inicio.plusDays(6);

    var existente = relatorios.buscarPeriodo(inicio, fim);
    if (existente.isPresent() && existente.get().status == StatusRelatorio.ENVIADO) {
      return Preparation.alreadySent(existente.get().id, inicio, fim);
    }

    Instant inicioInstant = inicio.atStartOfDay(zone).toInstant();
    Instant fimExclusivo = fim.plusDays(1).atStartOfDay(zone).toInstant();
    List<AvaliacaoEntity> itens = avaliacoes.buscarPeriodo(inicioInstant, fimExclusivo);

    int total = itens.size();
    int criticas = (int) itens.stream().filter(a -> a.urgencia == Urgencia.CRITICA).count();
    int atencao = (int) itens.stream().filter(a -> a.urgencia == Urgencia.ATENCAO).count();
    int normais = (int) itens.stream().filter(a -> a.urgencia == Urgencia.NORMAL).count();
    BigDecimal media =
        total == 0
            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.valueOf(itens.stream().mapToInt(a -> a.nota).average().orElse(0))
                .setScale(2, RoundingMode.HALF_UP);

    RelatorioSemanalEntity relatorio = existente.orElseGet(RelatorioSemanalEntity::new);
    if (relatorio.id == null) {
      relatorio.id = UUID.randomUUID();
    }

    relatorio.dataInicio = inicio;
    relatorio.dataFim = fim;
    relatorio.mediaNotas = media;
    relatorio.totalAvaliacoes = total;
    relatorio.totalCriticas = criticas;
    relatorio.totalAtencao = atencao;
    relatorio.totalNormais = normais;
    relatorio.status = StatusRelatorio.GERADO;
    relatorio.dataGeracao = Instant.now();
    relatorio.dataEnvio = null;
    relatorio.ultimoErro = null;
    relatorio.conteudo = html(relatorio, itens, zone);

    if (existente.isEmpty()) {
      relatorios.persist(relatorio);
    }
    relatorios.flush();

    return Preparation.ready(relatorio.id, inicio, fim, relatorio.conteudo, total, criticas);
  }

  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void marcarEnviado(UUID relatorioId) {
    RelatorioSemanalEntity relatorio = buscarRelatorio(relatorioId);
    relatorio.status = StatusRelatorio.ENVIADO;
    relatorio.dataEnvio = Instant.now();
    relatorio.ultimoErro = null;
    relatorios.flush();
  }

  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void marcarFalha(UUID relatorioId, RuntimeException falha) {
    RelatorioSemanalEntity relatorio = buscarRelatorio(relatorioId);
    relatorio.status = StatusRelatorio.FALHOU;
    relatorio.ultimoErro = descricaoFalha(falha);
    relatorios.flush();
  }

  private RelatorioSemanalEntity buscarRelatorio(UUID relatorioId) {
    return relatorios
        .findByIdOptional(relatorioId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "REPORT_NOT_FOUND", "O relatório " + relatorioId + " não foi encontrado."));
  }

  private String html(RelatorioSemanalEntity relatorio, List<AvaliacaoEntity> itens, ZoneId zone) {
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
        .append(relatorio.dataInicio)
        .append(" a ")
        .append(relatorio.dataFim)
        .append("</p>")
        .append("<p><strong>Total:</strong> ")
        .append(relatorio.totalAvaliacoes)
        .append(" | <strong>Média:</strong> ")
        .append(relatorio.mediaNotas)
        .append("</p>")
        .append("<ul><li>Críticas: ")
        .append(relatorio.totalCriticas)
        .append("</li><li>Atenção: ")
        .append(relatorio.totalAtencao)
        .append("</li><li>Normais: ")
        .append(relatorio.totalNormais)
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
      UUID reportId,
      LocalDate start,
      LocalDate end,
      String content,
      int totalFeedbacks,
      int criticalFeedbacks) {

    static Preparation alreadySent(UUID reportId, LocalDate start, LocalDate end) {
      return new Preparation("JA_ENVIADO", reportId, start, end, null, 0, 0);
    }

    static Preparation ready(
        UUID reportId,
        LocalDate start,
        LocalDate end,
        String content,
        int totalFeedbacks,
        int criticalFeedbacks) {
      return new Preparation(
          "PRONTO", reportId, start, end, content, totalFeedbacks, criticalFeedbacks);
    }

    public boolean shouldSend() {
      return "PRONTO".equals(status);
    }
  }
}
