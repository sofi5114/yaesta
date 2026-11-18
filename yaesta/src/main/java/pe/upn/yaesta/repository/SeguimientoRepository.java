package pe.upn.yaesta.repository;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.upn.yaesta.model.Seguimiento;

public interface SeguimientoRepository extends JpaRepository<Seguimiento, Long> {
  Optional<Seguimiento> findByClienteIdAndNegocioId(Long clienteId, Long negocioId);

  List<Seguimiento> findByClienteId(Long clienteId);

  long countByNegocioId(Long negocioId);
}
