package pe.upn.yaesta.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.upn.yaesta.model.EstadoRecordatorio;
import pe.upn.yaesta.model.Recordatorio;

public record RecordatorioResumen(
    Long id,
    Long turnoId,
    Long negocioId,
    String negocio,
    Long servicioId,
    String servicio,
    LocalDate fechaProgramada,
    EstadoRecordatorio estado,
    LocalDateTime enviadoEn
) {

    public static RecordatorioResumen de(Recordatorio recordatorio) {
        var turno = recordatorio.getTurno();

        return new RecordatorioResumen(
            recordatorio.getId(),
            turno.getId(),
            turno.getNegocio().getId(),
            turno.getNegocio().getNombre(),
            turno.getServicio().getId(),
            turno.getServicio().getNombre(),
            recordatorio.getFechaProgramada(),
            recordatorio.getEstado(),
            recordatorio.getEnviadoEn()
        );
    }
}