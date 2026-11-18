package pe.upn.yaesta.repository;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.upn.yaesta.model.Intercambio;

public interface IntercambioRepository extends JpaRepository<Intercambio, Long> {
  List<Intercambio> findTop50ByReceptorIdOrSolicitanteIdOrderByIdDesc(
      Long receptorId, Long solicitanteId);

  boolean existsByOrigenIdAndEstadoAndVenceEnAfter(Long id, String estado, LocalDateTime fecha);
}
