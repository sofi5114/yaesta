package pe.upn.yaesta.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.upn.yaesta.model.EventoTurno;

public interface EventoTurnoRepository extends JpaRepository<EventoTurno, Long> {
  List<EventoTurno> findByTurnoIdOrderByIdAsc(Long turnoId);
}
