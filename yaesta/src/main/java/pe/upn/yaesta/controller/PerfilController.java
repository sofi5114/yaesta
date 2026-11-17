package pe.upn.yaesta.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.upn.yaesta.dto.*;
import pe.upn.yaesta.service.PerfilService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/perfil")
public class PerfilController {
    private final PerfilService servicio;

    @GetMapping
    public Jsend<UsuarioResumen> consultar(@AuthenticationPrincipal Long usuarioId) {
        return Jsend.success(servicio.consultar(usuarioId));
    }

    @PatchMapping("/preferencias")
    public Jsend<UsuarioResumen> preferencias(
            @AuthenticationPrincipal Long usuarioId,
            @Valid @RequestBody PreferenciasRequest request) {
        return Jsend.success(servicio.preferencias(
            usuarioId, request.aceptaRecordatorios()
        ));
    }
}
