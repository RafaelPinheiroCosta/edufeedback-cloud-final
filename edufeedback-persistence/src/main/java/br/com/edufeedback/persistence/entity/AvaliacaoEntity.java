package br.com.edufeedback.persistence.entity;

import br.com.edufeedback.domain.Urgencia;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "avaliacao", schema = "edufeedback")
public class AvaliacaoEntity extends EntidadeAuditavel {
  @Id public UUID id;

  @Column(nullable = false, columnDefinition = "text")
  public String descricao;

  @Column(nullable = false)
  public short nota;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  public Urgencia urgencia;

  @Column(name = "data_envio", nullable = false)
  public Instant dataEnvio;

  @Column(name = "correlation_id", nullable = false)
  public UUID correlationId;
}
