package pe.upn.yaesta.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.upn.yaesta.model.Publicacion;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {
  Page<Publicacion> findByNegocioIdInAndVenceEnAfter(
      Collection<Long> ids, LocalDateTime fecha, Pageable pagina);

  Page<Publicacion> findByNegocioIdAndVenceEnAfter(Long id, LocalDateTime fecha, Pageable pagina);

  long countByNegocioIdAndCreadaEnAfter(Long id, LocalDateTime fecha);
}
