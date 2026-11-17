package pe.upn.yaesta.dto;

import jakarta.validation.constraints.*;

public record TurnoPresencialRequest(

    @NotNull(message = "Selecciona un servicio")
    @Positive(message = "El servicio debe tener un ID válido")
    Long servicioId,

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
    String nombreCliente

) {
}