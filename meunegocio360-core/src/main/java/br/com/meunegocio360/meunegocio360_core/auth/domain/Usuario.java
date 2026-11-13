package br.com.meunegocio360.meunegocio360_core.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class Usuario {
  @Id private UUID id;

  @Column(nullable = false, length = 120)
  private String nome;

  @Column(nullable = false, unique = true, length = 160)
  private String email;

  @Column(name = "senha_hash", nullable = false, length = 100)
  private String senhaHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Perfil perfil;

  @Column(nullable = false)
  private boolean ativo;

  @Column(name = "tentativas_falhas", nullable = false)
  private int tentativasFalhas;

  @Column(name = "bloqueado_ate")
  private Instant bloqueadoAte;

  @Column(name = "ultimo_login_em")
  private Instant ultimoLoginEm;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;

  @Version
  @Column(nullable = false)
  private long versao;

  protected Usuario() {}

  public Usuario(String nome, String email, String senhaHash, Perfil perfil) {
    this.id = UUID.randomUUID();
    this.nome = nome;
    this.email = email;
    this.senhaHash = senhaHash;
    this.perfil = perfil;
    this.ativo = true;
    this.criadoEm = Instant.now();
    this.atualizadoEm = this.criadoEm;
  }

  public void registrarFalha(Instant agora, int maxTentativas, int bloqueioMinutos) {
    tentativasFalhas++;
    if (tentativasFalhas >= maxTentativas) {
      bloqueadoAte = agora.plusSeconds(bloqueioMinutos * 60L);
    }
    atualizadoEm = agora;
  }

  public void registrarLogin(Instant agora) {
    tentativasFalhas = 0;
    bloqueadoAte = null;
    ultimoLoginEm = agora;
    atualizadoEm = agora;
  }

  public void limparBloqueioExpirado(Instant agora) {
    tentativasFalhas = 0;
    bloqueadoAte = null;
    atualizadoEm = agora;
  }

  public UUID getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public String getEmail() {
    return email;
  }

  public String getSenhaHash() {
    return senhaHash;
  }

  public Perfil getPerfil() {
    return perfil;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public int getTentativasFalhas() {
    return tentativasFalhas;
  }

  public Instant getBloqueadoAte() {
    return bloqueadoAte;
  }
}
