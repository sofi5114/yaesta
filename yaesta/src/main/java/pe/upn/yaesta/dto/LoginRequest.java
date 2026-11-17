package pe.upn.yaesta.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    @Size(max = 120, message = "El correo admite hasta 120 caracteres")
    String correo,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 72, message = "La contraseña es demasiado larga")
    String clave

) {
}