package br.com.meunegocio360.meunegocio360_core.auth.application;

import br.com.meunegocio360.meunegocio360_core.auth.domain.Perfil;
import br.com.meunegocio360.meunegocio360_core.auth.domain.Usuario;
import br.com.meunegocio360.meunegocio360_core.auth.domain.UsuarioStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class SeedDesenvolvimento implements CommandLineRunner {
  private final UsuarioStore usuarios;
  private final PasswordEncoder passwordEncoder;
  private final String senhaAdmin;
  private final String senhaUsuario;

  public SeedDesenvolvimento(
      UsuarioStore usuarios,
      PasswordEncoder passwordEncoder,
      @Value("${SEED_ADMIN_SENHA:}") String senhaAdmin,
      @Value("${SEED_USUARIO_SENHA:}") String senhaUsuario) {
    this.usuarios = usuarios;
    this.passwordEncoder = passwordEncoder;
    this.senhaAdmin = senhaAdmin;
    this.senhaUsuario = senhaUsuario;
  }

  @Override
  public void run(String... args) {
    if (senhaAdmin.isBlank() || senhaUsuario.isBlank()) {
      return;
    }
    criarSeAusente("admin@meunegocio360.com.br", "Administrador", senhaAdmin, Perfil.ADMIN);
    criarSeAusente(
        "usuario@meunegocio360.com.br", "Usuário de desenvolvimento", senhaUsuario, Perfil.USUARIO);
  }

  private void criarSeAusente(String email, String nome, String senha, Perfil perfil) {
    if (!usuarios.existsByEmail(email)) {
      usuarios.save(new Usuario(nome, email, passwordEncoder.encode(senha), perfil));
    }
  }
}
