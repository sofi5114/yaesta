package pe.upn.yaesta.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.upn.yaesta.dto.Jsend;
import pe.upn.yaesta.service.IntercambioService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plus/intercambios")
public class IntercambioController {
  private final IntercambioService servicio;

  public record Propuesta(@NotNull @Positive Long origenId, @NotNull @Positive Long destinoId) {}

  public record Decision(@NotNull Boolean aceptar) {}

  @GetMapping
  public Jsend<List<IntercambioService.Resumen>> listar(@AuthenticationPrincipal Long id) {
    return Jsend.success(servicio.listar(id));
  }

  @PostMapping
  public Jsend<IntercambioService.Resumen> proponer(
      @AuthenticationPrincipal Long id, @Valid @RequestBody Propuesta r) {
    return Jsend.success(servicio.proponer(id, r.origenId(), r.destinoId()));
  }

  @PatchMapping("/{intercambioId}")
  public Jsend<IntercambioService.Resumen> resolver(
      @AuthenticationPrincipal Long id,
      @PathVariable Long intercambioId,
      @Valid @RequestBody Decision r) {
    return Jsend.success(servicio.resolver(id, intercambioId, r.aceptar()));
  }
}
