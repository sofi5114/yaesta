package pe.upn.yaesta.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfiguracionColaRequest(

    @NotNull(message = "Indica si se aceptan turnos")
    Boolean aceptaTurnos,

    @NotNull(message = "Indica la tolerancia")
    @Min(value = 1, message = "La tolerancia mínima es 1 minuto")
    @Max(value = 30, message = "La tolerancia máxima es 30 minutos")
    Integer toleranciaMinutos

) {
}