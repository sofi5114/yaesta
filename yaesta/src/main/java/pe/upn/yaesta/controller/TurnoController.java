package pe.upn.yaesta.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import pe.upn.yaesta.dto.*;
import pe.upn.yaesta.service.TurnoService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TurnoController {

    private final TurnoService servicio;

    @GetMapping("/turnos")
    public Jsend<Pagina<TurnoResumen>> misTurnos(
            @AuthenticationPrincipal Long clienteId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {

        return Jsend.success(
            servicio.misTurnos(clienteId, pagina, tamano)
        );
    }

    @GetMapping("/turnos/{turnoId}")
    public Jsend<TurnoResumen> miTurno(
            @AuthenticationPrincipal Long clienteId,
            @PathVariable Long turnoId) {

        return Jsend.success(
            servicio.miTurno(clienteId, turnoId)
        );
    }

    @PostMapping("/negocios/{negocioId}/turnos")
    @ResponseStatus(HttpStatus.CREATED)
    public Jsend<TurnoResumen> solicitar(
            @PathVariable Long negocioId,
            @AuthenticationPrincipal Long clienteId,
            @Valid @RequestBody TurnoRequest request) {

        return Jsend.success(
            servicio.solicitar(negocioId, clienteId, request)
        );
    }

    @DeleteMapping("/negocios/{negocioId}/turnos/{turnoId}")
    public Jsend<TurnoResumen> cancelar(
            @PathVariable Long negocioId,
            @PathVariable Long turnoId,
            @AuthenticationPrincipal Long clienteId) {

        return Jsend.success(
            servicio.cancelar(negocioId, turnoId, clienteId)
        );
    }

    @GetMapping("/negocios/{negocioId}/cola")
    public Jsend<Pagina<TurnoResumen>> cola(
            @PathVariable Long negocioId,
            @AuthenticationPrincipal Long propietarioId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {

        return Jsend.success(
            servicio.cola(negocioId, propietarioId, pagina, tamano)
        );
    }

    @GetMapping("/negocios/{negocioId}/cola/configuracion")
    public Jsend<ConfiguracionColaResumen> configuracion(
            @PathVariable Long negocioId,
            @AuthenticationPrincipal Long propietarioId) {

        return Jsend.success(
            servicio.configuracion(negocioId, propietarioId)
        );
    }

    @PatchMapping("/negocios/{negocioId}/cola/configuracion")
    public Jsend<ConfiguracionColaResumen> configurar(
            @PathVariable Long negocioId,
            @AuthenticationPrincipal Long propietarioId,
            @Valid @RequestBody ConfiguracionColaRequest request) {

        return Jsend.success(
            servicio.configurar(negocioId, propietarioId, request)
        );
    }

    @PostMapping("/negocios/{negocioId}/cola/presenciales")
    @ResponseStatus(HttpStatus.CREATED)
    public Jsend<TurnoResumen> registrarPresencial(
            @PathVariable Long negocioId,
            @AuthenticationPrincipal Long propietarioId,
            @Valid @RequestBody TurnoPresencialRequest request) {

        return Jsend.success(
            servicio.registrarPresencial(
                negocioId, propietarioId, request
            )
        );
    }

    @PostMapping("/negocios/{negocioId}/cola/llamados")
    public Jsend<TurnoResumen> llamarSiguiente(
            @PathVariable Long negocioId,
            @AuthenticationPrincipal Long propietarioId) {

        return Jsend.success(
            servicio.llamarSiguiente(negocioId, propietarioId)
        );
    }

    @PatchMapping("/negocios/{negocioId}/cola/turnos/{turnoId}/llegada")
    public Jsend<TurnoResumen> registrarLlegada(
            @PathVariable Long negocioId,
            @PathVariable Long turnoId,
            @AuthenticationPrincipal Long propietarioId) {

        return Jsend.success(
            servicio.registrarLlegada(
                negocioId, turnoId, propietarioId
            )
        );
    }

    @PatchMapping("/negocios/{negocioId}/cola/turnos/{turnoId}/inicio")
    public Jsend<TurnoResumen> iniciar(
            @PathVariable Long negocioId,
            @PathVariable Long turnoId,
            @AuthenticationPrincipal Long propietarioId) {

        return Jsend.success(
            servicio.iniciar(negocioId, turnoId, propietarioId)
        );
    }

    @PatchMapping("/negocios/{negocioId}/cola/turnos/{turnoId}/finalizacion")
    public Jsend<TurnoResumen> completar(
            @PathVariable Long negocioId,
            @PathVariable Long turnoId,
            @AuthenticationPrincipal Long propietarioId) {

        return Jsend.success(
            servicio.completar(negocioId, turnoId, propietarioId)
        );
    }

    @PatchMapping("/negocios/{negocioId}/cola/turnos/{turnoId}/ausencia")
    public Jsend<TurnoResumen> marcarAusente(
            @PathVariable Long negocioId,
            @PathVariable Long turnoId,
            @AuthenticationPrincipal Long propietarioId) {

        return Jsend.success(
            servicio.marcarAusente(
                negocioId, turnoId, propietarioId
            )
        );
    }
}