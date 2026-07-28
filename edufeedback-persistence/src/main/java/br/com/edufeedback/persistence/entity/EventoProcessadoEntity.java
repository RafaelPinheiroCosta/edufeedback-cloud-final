package br.com.edufeedback.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evento_processado", schema = "edufeedback")
public class EventoProcessadoEntity {
  @Id public UUID id;

  @Column(name = "event_id", nullable = false)
  public UUID eventId;

  @Column(nullable = false, length = 100)
  public String consumer;

  @Column(name = "event_type", nullable = false, length = 100)
  public String eventType;

  @Column(nullable = false, length = 20)
  public String status;

  @Column(name = "processed_at", nullable = false)
  public Instant processedAt;

  @Column(name = "correlation_id", nullable = false)
  public UUID correlationId;
}
