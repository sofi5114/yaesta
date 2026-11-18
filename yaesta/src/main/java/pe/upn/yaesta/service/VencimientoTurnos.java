package pe.upn.yaesta.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.upn.yaesta.model.EstadoTurno;
import pe.upn.yaesta.repository.TurnoRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class VencimientoTurnos {
  private final TurnoRepository turnos;
  private final TurnoService servicio;

  @Scheduled(fixedDelayString = "${yaesta.cola.vencimiento-ms:15000}")
  public void revisar() {
    var fecha = LocalDateTime.now(ZoneId.of("America/Lima"));
    for (var candidato : turnos.findByEstadoAndLlegadaEnIsNull(EstadoTurno.LLAMADO)) {
      try {
        servicio.vencerAusencia(candidato.getNegocio().getId(), candidato.getId(), fecha);
      } catch (Exception error) {
        log.error("No se pudo revisar el vencimiento del turno {}", candidato.getId(), error);
      }
    }
  }
}
