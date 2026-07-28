package br.com.edufeedback.persistence.entity;

import br.com.edufeedback.domain.StatusRelatorio;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "relatorio_semanal", schema = "edufeedback")
public class RelatorioSemanalEntity extends EntidadeAuditavel {
  @Id public UUID id;

  @Column(name = "data_inicio", nullable = false)
  public LocalDate dataInicio;

  @Column(name = "data_fim", nullable = false)
  public LocalDate dataFim;

  @Column(name = "media_notas", nullable = false, precision = 4, scale = 2)
  public BigDecimal mediaNotas;

  @Column(name = "total_avaliacoes", nullable = false)
  public int totalAvaliacoes;

  @Column(name = "total_criticas", nullable = false)
  public int totalCriticas;

  @Column(name = "total_atencao", nullable = false)
  public int totalAtencao;

  @Column(name = "total_normais", nullable = false)
  public int totalNormais;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  public StatusRelatorio status;

  @Column(nullable = false, columnDefinition = "text")
  public String conteudo;

  @Column(name = "data_geracao", nullable = false)
  public Instant dataGeracao;

  @Column(name = "data_envio")
  public Instant dataEnvio;

  @Column(name = "ultimo_erro", columnDefinition = "text")
  public String ultimoErro;

  @Version public long versao;
}
