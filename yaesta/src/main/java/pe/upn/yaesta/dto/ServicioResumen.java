package pe.upn.yaesta.dto;

import java.math.BigDecimal;

import pe.upn.yaesta.model.Servicio;

public record ServicioResumen(
    Long id,
    Long negocioId,
    String nombre,
    BigDecimal precio,
    int duracionMinutos,
    Integer diasRecordatorio,
    boolean activo
) {

    public static ServicioResumen de(Servicio servicio) {
        return new ServicioResumen(
            servicio.getId(),
            servicio.getNegocio().getId(),
            servicio.getNombre(),
            servicio.getPrecio(),
            servicio.getDuracionMinutos(),
            servicio.getDiasRecordatorio(),
            servicio.isActivo()
        );
    }
}
