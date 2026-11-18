package pe.upn.yaesta.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.upn.yaesta.dto.*;
import pe.upn.yaesta.service.PlusService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plus")
public class PlusController {
  private final PlusService servicio;

  @GetMapping("/planes")
  public Jsend<PlusService.Planes> planes() {
    return Jsend.success(servicio.planes());
  }

  @GetMapping("/membresia")
  public Jsend<PlusService.Estado> estado(
      @AuthenticationPrincipal Long id, @RequestParam(required = false) Long negocioId) {
    return Jsend.success(servicio.estado(id, negocioId));
  }

  @PostMapping("/prueba")
  public Jsend<PlusService.Estado> prueba(
      @AuthenticationPrincipal Long id, @RequestParam(required = false) Long negocioId) {
    return Jsend.success(servicio.activarPrueba(id, negocioId));
  }

  @DeleteMapping("/membresia")
  public Jsend<PlusService.Estado> cancelar(
      @AuthenticationPrincipal Long id, @RequestParam(required = false) Long negocioId) {
    return Jsend.success(servicio.cancelar(id, negocioId));
  }

  @PutMapping("/negocios/{negocioId}/seguir")
  public Jsend<PlusService.Estado> seguir(
      @AuthenticationPrincipal Long id,
      @PathVariable Long negocioId,
      @RequestParam(defaultValue = "false") boolean alertas) {
    return Jsend.success(servicio.seguir(id, negocioId, alertas));
  }

  @DeleteMapping("/negocios/{negocioId}/seguir")
  public Jsend<PlusService.Estado> dejar(
      @AuthenticationPrincipal Long id, @PathVariable Long negocioId) {
    return Jsend.success(servicio.dejarSeguir(id, negocioId));
  }

  @GetMapping("/novedades")
  public Jsend<Pagina<PlusService.Novedad>> feed(
      @AuthenticationPrincipal Long id, @RequestParam(defaultValue = "0") int pagina) {
    return Jsend.success(servicio.novedades(id, pagina));
  }

  @GetMapping("/negocios/{negocioId}/publicaciones")
  public Jsend<Pagina<PlusService.Novedad>> publicaciones(
      @PathVariable Long negocioId, @RequestParam(defaultValue = "0") int pagina) {
    return Jsend.success(servicio.publicaciones(negocioId, pagina));
  }

  @PostMapping("/negocios/{negocioId}/publicaciones")
  public Jsend<PlusService.Novedad> publicar(
      @AuthenticationPrincipal Long id,
      @PathVariable Long negocioId,
      @Valid @RequestBody PublicacionRequest r) {
    return Jsend.success(servicio.publicar(id, negocioId, r));
  }

  @DeleteMapping("/negocios/{negocioId}/publicaciones/{publicacionId}")
  public Jsend<String> borrar(
      @AuthenticationPrincipal Long id,
      @PathVariable Long negocioId,
      @PathVariable Long publicacionId) {
    servicio.borrarPublicacion(id, negocioId, publicacionId);
    return Jsend.success("Publicación retirada");
  }
}
