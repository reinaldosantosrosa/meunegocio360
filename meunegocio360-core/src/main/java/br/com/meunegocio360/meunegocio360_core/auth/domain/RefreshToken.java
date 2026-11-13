package br.com.meunegocio360.meunegocio360_core.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(name = "familia_id", nullable = false)
  private UUID familiaId;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expira_em", nullable = false)
  private Instant expiraEm;

  @Column(name = "revogado_em")
  private Instant revogadoEm;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm;

  protected RefreshToken() {}

  public RefreshToken(
      Usuario usuario, UUID familiaId, String tokenHash, Instant expiraEm, Instant agora) {
    this.id = UUID.randomUUID();
    this.usuario = usuario;
    this.familiaId = familiaId;
    this.tokenHash = tokenHash;
    this.expiraEm = expiraEm;
    this.criadoEm = agora;
  }

  public void revogar(Instant agora) {
    revogadoEm = agora;
  }

  public UUID getFamiliaId() {
    return familiaId;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public Instant getExpiraEm() {
    return expiraEm;
  }

  public Instant getRevogadoEm() {
    return revogadoEm;
  }
}
