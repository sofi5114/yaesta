package pe.upn.yaesta.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import pe.upn.yaesta.model.EstadoTurno;
import pe.upn.yaesta.model.OrigenTurno;
import pe.upn.yaesta.model.Turno;

public record TurnoResumen(
    Long id,
    Long negocioId,
    String negocio,
    Long servicioId,
    String servicio,
    String nombreCliente,
    EstadoTurno estado,
    OrigenTurno origen,
    LocalDateTime solicitadoEn,
    LocalDateTime llegadaEn,
    LocalDateTime llamadoEn,
    LocalDateTime inicioEn,
    LocalDateTime finEn,
    int toleranciaMinutos,
    BigDecimal precioServicio,
    Long posicion,
    Long esperaEstimadaMinutos,
    LocalDateTime finEstimado,
    boolean demoraSignificativa,
    Long esperaInicialMinutos,
    LocalDateTime noLlamarAntesDe) {

  public static TurnoResumen de(Turno turno, Long posicion, Long esperaEstimadaMinutos) {

    String nombreCliente =
        turno.getCliente() == null ? turno.getNombrePresencial() : turno.getCliente().getNombre();

    return new TurnoResumen(
        turno.getId(),
        turno.getNegocio().getId(),
        turno.getNegocio().getNombre(),
        turno.getServicio().getId(),
        turno.getServicio().getNombre(),
        nombreCliente,
        turno.getEstado(),
        turno.getOrigen(),
        turno.getSolicitadoEn(),
        turno.getLlegadaEn(),
        turno.getLlamadoEn(),
        turno.getInicioEn(),
        turno.getFinEn(),
        turno.getToleranciaMinutos(),
        turno.getPrecioServicio(),
        posicion,
        esperaEstimadaMinutos,
        turno.getFinEstimado(),
        turno.getEstado() == EstadoTurno.EN_ESPERA
            && turno.demoraSignificativa(
                esperaEstimadaMinutos,
                java.time.LocalDateTime.now(java.time.ZoneId.of("America/Lima"))),
        turno.getEsperaInicialMinutos(),
        turno.getNoLlamarAntesDe());
  }
}
