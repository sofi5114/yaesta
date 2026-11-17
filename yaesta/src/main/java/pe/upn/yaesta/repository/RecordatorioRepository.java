package pe.upn.yaesta.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import pe.upn.yaesta.model.EstadoRecordatorio;
import pe.upn.yaesta.model.Recordatorio;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordatorioRepository
        extends JpaRepository<Recordatorio, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM Recordatorio r
            WHERE r.id = :id
              AND r.estado = :estado
              AND r.fechaProgramada <= :fecha
            """)
    Optional<Recordatorio> bloquearParaEnvio(
            @Param("id") Long id,
            @Param("estado") EstadoRecordatorio estado,
            @Param("fecha") LocalDate fecha);

    Page<Recordatorio> findByTurnoClienteId(
            Long clienteId,
            Pageable pagina);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Recordatorio> findByIdAndTurnoClienteId(
            Long id,
            Long clienteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Recordatorio> findByTurnoClienteIdAndTurnoServicioIdAndEstadoIn(
            Long clienteId,
            Long servicioId,
            Collection<EstadoRecordatorio> estados);

    List<Recordatorio> findTop50ByEstadoAndFechaProgramadaLessThanEqualOrderByFechaProgramadaAscIdAsc(
            EstadoRecordatorio estado,
            LocalDate fecha);

            List<Recordatorio>
    findByTurnoClienteIdAndFechaProgramadaBetweenOrderByFechaProgramadaAscIdAsc(
        Long clienteId,
        LocalDate inicio,
        LocalDate fin
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Recordatorio> findByTurnoClienteIdAndEstadoIn(
        Long clienteId, Collection<EstadoRecordatorio> estados
    );
}
