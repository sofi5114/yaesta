package pe.upn.yaesta.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.upn.yaesta.dto.UsuarioResumen;
import pe.upn.yaesta.model.*;
import pe.upn.yaesta.repository.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerfilService {
    private final UsuarioRepository usuarios;
    private final RecordatorioRepository recordatorios;

    public UsuarioResumen consultar(Long usuarioId) {
        return UsuarioResumen.de(obtener(usuarioId));
    }

    @Transactional
    public UsuarioResumen preferencias(Long usuarioId, boolean acepta) {
        Usuario usuario = obtener(usuarioId);

        if (!acepta) {
            recordatorios.findByTurnoClienteIdAndEstadoIn(
                usuarioId,
                List.of(EstadoRecordatorio.PENDIENTE, EstadoRecordatorio.ERROR)
            ).forEach(recordatorio -> recordatorio.cancelar());
        }

        usuario.cambiarRecordatorios(acepta);
        return UsuarioResumen.de(usuario);
    }

    private Usuario obtener(Long id) {
        return usuarios.findById(id)
            .filter(usuario -> usuario.isActivo())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Tu sesión ya no es válida"
            ));
    }
}
