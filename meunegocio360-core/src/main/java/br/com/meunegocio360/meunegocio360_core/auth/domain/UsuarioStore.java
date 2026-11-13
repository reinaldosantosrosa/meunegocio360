package br.com.meunegocio360.meunegocio360_core.auth.domain;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioStore {
  Optional<Usuario> findByEmailForUpdate(String email);

  Optional<Usuario> findById(UUID id);

  boolean existsByEmail(String email);

  Usuario save(Usuario usuario);
}
