package br.com.edufeedback.persistence.entity;

import br.com.edufeedback.domain.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notificacao", schema = "edufeedback")
public class NotificacaoEntity extends EntidadeAuditavel {
  @Id public UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "avaliacao_id", nullable = false)
  public AvaliacaoEntity avaliacao;

  @Column(name = "event_id", nullable = false)
  public UUID eventId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  public TipoNotificacao tipo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  public StatusNotificacao status;

  @Column(nullable = false, length = 320)
  public String destinatario;

  @Column(nullable = false, columnDefinition = "text")
  public String mensagem;

  @Column(nullable = false)
  public int tentativas;

  @Column(name = "ultimo_erro", columnDefinition = "text")
  public String ultimoErro;

  @Column(name = "data_envio")
  public Instant dataEnvio;

  @Version public long versao;
}
