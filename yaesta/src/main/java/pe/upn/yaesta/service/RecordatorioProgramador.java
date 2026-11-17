package pe.upn.yaesta.service;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.upn.yaesta.model.EstadoRecordatorio;
import pe.upn.yaesta.repository.RecordatorioRepository;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "yaesta.correo.habilitado",
    havingValue = "true"
)
public class RecordatorioProgramador {

    private final RecordatorioRepository recordatorios;
    private final CorreoService correoService;

    @Scheduled(fixedDelay = 60000)
    public void enviarPendientes() {
        var fecha = LocalDate.now(ZoneId.of("America/Lima"));

        var pendientes = recordatorios
            .findTop50ByEstadoAndFechaProgramadaLessThanEqualOrderByFechaProgramadaAscIdAsc(
                EstadoRecordatorio.PENDIENTE,
                fecha
            );

        for (var recordatorio : pendientes) {
            try {
                correoService.enviarRecordatorio(recordatorio.getId());
            } catch (Exception error) {
                log.error(
                    "No se pudo procesar el recordatorio {}",
                    recordatorio.getId(),
                    error
                );
            }
        }
    }
}