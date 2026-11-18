package pe.upn.yaesta.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pe.upn.yaesta.dto.*;
import pe.upn.yaesta.service.AlternativasService;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api")
public class AlternativasController {
  private final AlternativasService servicio;

  public record Ubicacion(
      @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
      @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud,
      @Min(0) @Max(30) int descuento,
      @Min(0) @Max(100) int cupos) {}

  public record Cambio(
      @NotNull @Positive Long servicioId, @NotNull @DecimalMin("0") BigDecimal precioAceptado, @Min(1) @Max(60) int minutosTraslado) {}

  @GetMapping("/negocios/{negocioId}/cola/ubicacion")
  public Jsend<AlternativasService.Configuracion> config(
      @AuthenticationPrincipal Long id, @PathVariable Long negocioId) {
    return Jsend.success(servicio.configuracion(id, negocioId));
  }

  @PatchMapping("/negocios/{negocioId}/cola/ubicacion")
  public Jsend<AlternativasService.Configuracion> config(
      @AuthenticationPrincipal Long id,
      @PathVariable Long negocioId,
      @Valid @RequestBody Ubicacion r) {
    return Jsend.success(
        servicio.configurar(id, negocioId, r.latitud(), r.longitud(), r.descuento(), r.cupos()));
  }

  @GetMapping("/turnos/{turnoId}/alternativas")
  public Jsend<List<AlternativasService.Alternativa>> buscar(
      @AuthenticationPrincipal Long id,
      @PathVariable Long turnoId,
      @RequestParam @DecimalMin("-90") @DecimalMax("90") double latitud,
      @RequestParam @DecimalMin("-180") @DecimalMax("180") double longitud) {
    return Jsend.success(servicio.buscar(id, turnoId, latitud, longitud));
  }

  @PostMapping("/turnos/{turnoId}/derivacion")
  public Jsend<TurnoResumen> cambiar(
      @AuthenticationPrincipal Long id, @PathVariable Long turnoId, @Valid @RequestBody Cambio r) {
    return Jsend.success(servicio.cambiar(id, turnoId, r.servicioId(), r.precioAceptado(), r.minutosTraslado()));
  }
}
