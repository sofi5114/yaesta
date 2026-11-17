package pe.upn.yaesta.dto;

import pe.upn.yaesta.model.Rol;
import pe.upn.yaesta.model.Usuario;

public record UsuarioResumen(
    Long id,
    String nombre,
    String correo,
    Rol rol,
    boolean aceptaRecordatorios
) {

    public static UsuarioResumen de(Usuario usuario) {
        return new UsuarioResumen(
            usuario.getId(),
            usuario.getNombre(),
            usuario.getCorreo(),
            usuario.getRol(),
            usuario.isAceptaRecordatorios()
        );
    }
}