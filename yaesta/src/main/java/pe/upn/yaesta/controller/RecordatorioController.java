package pe.upn.yaesta.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import pe.upn.yaesta.dto.Jsend;
import pe.upn.yaesta.dto.Pagina;
import pe.upn.yaesta.dto.RecordatorioResumen;
import pe.upn.yaesta.service.RecordatorioService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recordatorios")
public class RecordatorioController {

    private final RecordatorioService servicio;

    @GetMapping
    public Jsend<Pagina<RecordatorioResumen>> listar(
            @AuthenticationPrincipal Long clienteId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {

        return Jsend.success(
            servicio.misRecordatorios(clienteId, pagina, tamano)
        );
    }

    @DeleteMapping("/{recordatorioId}")
    public Jsend<RecordatorioResumen> cancelar(
            @AuthenticationPrincipal Long clienteId,
            @PathVariable Long recordatorioId) {

        return Jsend.success(
            servicio.cancelar(clienteId, recordatorioId)
        );
    }

    @GetMapping("/calendario")
public Jsend<List<RecordatorioResumen>> calendario(
        @AuthenticationPrincipal Long clienteId,
        @RequestParam int anio,
        @RequestParam int mes) {

    return Jsend.success(
        servicio.calendario(clienteId, anio, mes)
    );
}
}