package pe.upn.yaesta.dto;

import jakarta.validation.constraints.*;

public record PublicacionRequest(
    @NotBlank @Size(max = 100) String titulo,
    @NotBlank @Size(max = 1000) String mensaje,
    @Min(1) @Max(30) int vigenciaDias) {}
