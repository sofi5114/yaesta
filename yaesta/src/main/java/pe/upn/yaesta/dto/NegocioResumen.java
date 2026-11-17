package pe.upn.yaesta.dto;

import pe.upn.yaesta.model.Negocio;

public record NegocioResumen(
    Long id,
    String nombre,
    String categoria,
    String distrito
) {

    public static NegocioResumen de(Negocio negocio) {
        return new NegocioResumen(
            negocio.getId(),
            negocio.getNombre(),
            negocio.getCategoria(),
            negocio.getDistrito()
        );
    }
}