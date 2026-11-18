package pe.upn.yaesta.repository;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.upn.yaesta.model.EstadoTurno;
import pe.upn.yaesta.model.Turno;

public interface TurnoRepository extends JpaRepository<Turno, Long> {

  @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "negocio")
  java.util.List<Turno> findByEstadoAndLlegadaEnIsNull(EstadoTurno estado);

  @org.springframework.data.jpa.repository.Query(
      "select t from Turno t where t.negocio.id = :negocioId and t.estado in :estados order by"
          + " coalesce(t.ordenCola,t.id),t.id")
  java.util.List<Turno> colaOrdenada(
      @org.springframework.data.repository.query.Param("negocioId") Long negocioId,
      @org.springframework.data.repository.query.Param("estados") Collection<EstadoTurno> estados);

  Page<Turno> findByClienteId(Long clienteId, Pageable pagina);

  Page<Turno> findByNegocioIdAndEstadoIn(
      Long negocioId, Collection<EstadoTurno> estados, Pageable pagina);

  Optional<Turno> findByIdAndClienteId(Long id, Long clienteId);

  Optional<Turno> findByIdAndNegocioId(Long id, Long negocioId);

  Optional<Turno> findFirstByNegocioIdAndEstadoOrderByIdAsc(Long negocioId, EstadoTurno estado);

  boolean existsByNegocioIdAndClienteIdAndEstadoIn(
      Long negocioId, Long clienteId, Collection<EstadoTurno> estados);

  boolean existsByNegocioIdAndEstadoIn(Long negocioId, Collection<EstadoTurno> estados);

  long countByNegocioIdAndEstadoInAndIdLessThan(
      Long negocioId, Collection<EstadoTurno> estados, Long id);

  java.util.List<Turno> findByNegocioIdAndEstadoInAndIdLessThanOrderByIdAsc(
      Long negocioId, java.util.Collection<EstadoTurno> estados, Long turnoId);
}
