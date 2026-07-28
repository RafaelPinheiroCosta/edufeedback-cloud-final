package br.com.edufeedback.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@MappedSuperclass
public abstract class EntidadeAuditavel {
  @Column(name = "criado_em", nullable = false, updatable = false)
  public Instant criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  public Instant atualizadoEm;

  @PrePersist
  void prePersist() {
    var agora = Instant.now();
    if (criadoEm == null) criadoEm = agora;
    atualizadoEm = agora;
  }

  @PreUpdate
  void preUpdate() {
    atualizadoEm = Instant.now();
  }
}
