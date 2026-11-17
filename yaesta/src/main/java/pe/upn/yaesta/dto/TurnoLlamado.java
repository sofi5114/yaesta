package pe.upn.yaesta.dto;

import java.time.LocalDateTime;

public record TurnoLlamado(
    Long turnoId,
    String correo,
    String nombreCliente,
    String negocio,
    LocalDateTime limiteLlegada
) {
}