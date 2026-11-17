package pe.upn.yaesta.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.upn.yaesta.model.Servicio;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    Page<Servicio> findByNegocioIdAndActivoTrue(
        Long negocioId, Pageable pagina
    );

    Optional<Servicio> findByIdAndNegocioIdAndActivoTrue(
        Long id, Long negocioId
    );
}