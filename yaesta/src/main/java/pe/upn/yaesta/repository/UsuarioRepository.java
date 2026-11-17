package pe.upn.yaesta.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.upn.yaesta.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);
}