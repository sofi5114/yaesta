package pe.upn.yaesta.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

public record ServicioRequest(

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
    String nombre,

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0", message = "El precio no puede ser negativo")
    @Digits(integer = 8, fraction = 2,
            message = "El precio admite hasta 8 enteros y 2 decimales")
    BigDecimal precio,

    @NotNull(message = "La duración es obligatoria")
    @Min(value = 1, message = "La duración debe ser mayor que cero")
    Integer duracionMinutos,

    @Min(value = 1, message = "Los días deben ser mayores que cero")
    Integer diasRecordatorio

) {
}