package pe.upn.yaesta.dto;

public record LoginResponse(
    String token,
    UsuarioResumen usuario
) {
}