package br.com.meunegocio360.meunegocio360_core.auth.infra;

import br.com.meunegocio360.meunegocio360_core.auth.domain.Usuario;
import br.com.meunegocio360.meunegocio360_core.auth.domain.UsuarioStore;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID>, UsuarioStore {
  Optional<Usuario> findByEmail(String email);

  boolean existsByEmail(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select usuario from Usuario usuario where usuario.email = :email")
  Optional<Usuario> findByEmailForUpdate(@Param("email") String email);
}
