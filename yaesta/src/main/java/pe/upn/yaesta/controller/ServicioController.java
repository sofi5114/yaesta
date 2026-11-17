package pe.upn.yaesta.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import pe.upn.yaesta.dto.Jsend;
import pe.upn.yaesta.dto.Pagina;
import pe.upn.yaesta.dto.ServicioRequest;
import pe.upn.yaesta.dto.ServicioResumen;
import pe.upn.yaesta.service.ServicioService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/negocios/{negocioId}/servicios")
public class ServicioController {

    private final ServicioService servicio;

    public ServicioController(ServicioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public ResponseEntity<Jsend<Pagina<ServicioResumen>>> listar(
            @PathVariable Long negocioId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {

        return ResponseEntity.ok(
                Jsend.success(servicio.listar(negocioId, pagina, tamano)));
    }

    @GetMapping("/{servicioId}")
    public ResponseEntity<Jsend<ServicioResumen>> buscar(
            @PathVariable Long negocioId,
            @PathVariable Long servicioId) {

        return ResponseEntity.ok(
                Jsend.success(servicio.buscar(negocioId, servicioId)));
    }

    @PostMapping
    public ResponseEntity<Jsend<ServicioResumen>> crear(
            @PathVariable Long negocioId,
            @AuthenticationPrincipal Long usuarioId,
            @Valid @RequestBody ServicioRequest request) {

        ServicioResumen creado = servicio.crear(
                negocioId, usuarioId, request);

        URI ubicacion = URI.create(
                "/api/negocios/" + negocioId + "/servicios/" + creado.id());

        return ResponseEntity.created(ubicacion)
                .body(Jsend.success(creado));
    }

    @PutMapping("/{servicioId}")
    public ResponseEntity<Jsend<ServicioResumen>> actualizar(
            @PathVariable Long negocioId,
            @PathVariable Long servicioId,
            @AuthenticationPrincipal Long usuarioId,
            @Valid @RequestBody ServicioRequest request) {

        return ResponseEntity.ok(
                Jsend.success(
                        servicio.actualizar(
                                negocioId, servicioId, usuarioId, request)));
    }

    @DeleteMapping("/{servicioId}")
    public ResponseEntity<Void> desactivar(
            @PathVariable Long negocioId,
            @PathVariable Long servicioId,
            @AuthenticationPrincipal Long usuarioId) {

        servicio.desactivar(negocioId, servicioId, usuarioId);

        return ResponseEntity.noContent().build();
    }
}