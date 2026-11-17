package pe.upn.yaesta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.upn.yaesta.model.Negocio;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NegocioRepository extends JpaRepository<Negocio, Long> {
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT n FROM Negocio n WHERE n.id = :id")
Optional<Negocio> bloquearPorId(@Param("id") Long id
);

List<Negocio> findByPropietarioIdOrderByNombreAscIdAsc(
    Long propietarioId
);
}