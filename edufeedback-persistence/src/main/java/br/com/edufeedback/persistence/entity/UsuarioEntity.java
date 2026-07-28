package br.com.edufeedback.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "usuario", schema = "edufeedback")
public class UsuarioEntity extends EntidadeAuditavel {
  @Id public UUID id;

  @Column(nullable = false, unique = true, length = 100)
  public String username;

  @Column(name = "password_hash", nullable = false, length = 100)
  public String passwordHash;

  @Column(nullable = false, length = 50)
  public String role;

  @Column(nullable = false)
  public boolean ativo;
}
