package pe.upn.yaesta.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import pe.upn.yaesta.dto.Jsend;
import pe.upn.yaesta.dto.NegocioResumen;
import pe.upn.yaesta.service.NegocioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/negocios")
public class NegocioController {

    private final NegocioService servicio;

    public NegocioController(NegocioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Jsend<List<NegocioResumen>> listar() {
        return Jsend.success(servicio.listar());
    }

    @GetMapping("/{id}")
    public Jsend<NegocioResumen> buscar(@PathVariable Long id) {
        return Jsend.success(servicio.buscar(id));
    }

    @GetMapping("/propios")
    public Jsend<List<NegocioResumen>> misNegocios(
            @AuthenticationPrincipal Long propietarioId) {

        return Jsend.success(
                servicio.misNegocios(propietarioId));
    }
}