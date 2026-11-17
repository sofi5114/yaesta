package pe.upn.yaesta.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TurnoRequest(

    @NotNull(message = "Selecciona un servicio")
    @Positive(message = "El servicio debe tener un ID válido")
    Long servicioId

) {
}