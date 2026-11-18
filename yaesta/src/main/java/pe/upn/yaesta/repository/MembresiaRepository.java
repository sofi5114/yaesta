package pe.upn.yaesta.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.upn.yaesta.model.Membresia;

public interface MembresiaRepository extends JpaRepository<Membresia, Long> {
  Optional<Membresia> findByClave(String clave);
}
