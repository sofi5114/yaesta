package pe.upn.yaesta.dto;

import pe.upn.yaesta.model.Negocio;

public record ConfiguracionColaResumen(
    Long negocioId,
    boolean aceptaTurnos,
    int toleranciaMinutos
) {

    public static ConfiguracionColaResumen de(Negocio negocio) {
        return new ConfiguracionColaResumen(
            negocio.getId(),
            negocio.isAceptaTurnos(),
            negocio.getToleranciaMinutos()
        );
    }
}