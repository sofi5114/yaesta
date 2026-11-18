package pe.upn.yaesta.service;

import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.upn.yaesta.dto.*;
import pe.upn.yaesta.model.*;
import pe.upn.yaesta.repository.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlusService {
  private final MembresiaRepository membresias;
  private final SeguimientoRepository seguimientos;
  private final PublicacionRepository publicaciones;
  private final UsuarioRepository usuarios;
  private final NegocioRepository negocios;
  private final NegocioService negocioService;

  @Value("${yaesta.plus.demo:false}")
  private boolean demo;

  @Value("${yaesta.plus.precio-cliente:9.90}")
  private java.math.BigDecimal precioCliente;

  @Value("${yaesta.plus.precio-negocio:}")
  private String precioNegocio;

  public record Planes(
      java.math.BigDecimal clienteMensual,
      java.math.BigDecimal negocioMensual,
      String moneda,
      boolean cobrosHabilitados,
      boolean saldoHabilitado) {}

  public Planes planes() {
    java.math.BigDecimal negocio =
        precioNegocio.isBlank() ? null : new java.math.BigDecimal(precioNegocio);
    return new Planes(precioCliente, negocio, "PEN", false, false);
  }

  public static LocalDateTime ahora() {
    return LocalDateTime.now(ZoneId.of("America/Lima"));
  }

  public record Estado(
      boolean activa,
      boolean demostracion,
      boolean permitePrueba,
      LocalDateTime venceEn,
      long seguidores,
      boolean siguiendo,
      boolean alertas) {}

  public record Novedad(
      Long id,
      Long negocioId,
      String negocio,
      String titulo,
      String mensaje,
      LocalDateTime creadaEn,
      LocalDateTime venceEn) {}

  public Estado estado(Long usuarioId, Long negocioId) {
    Usuario usuario = usuario(usuarioId);
    if (negocioId == null && usuario.getRol() != Rol.CLIENTE)
      throw error("Selecciona uno de tus negocios");
    if (negocioId != null) negocioService.buscarPorId(negocioId);
    var m = membresias.findByClave(clave(usuarioId, negocioId)).orElse(null);
    var s =
        negocioId == null
            ? null
            : seguimientos.findByClienteIdAndNegocioId(usuarioId, negocioId).orElse(null);
    boolean propia =
        negocioId == null
            || negocioService.buscarPorId(negocioId).getPropietario() != null
                && negocioService.buscarPorId(negocioId).getPropietario().getId().equals(usuarioId);
    return new Estado(
        m != null && m.vigente(ahora()),
        m != null && m.isDemostracion(),
        demo && m == null && propia,
        m == null ? null : m.getFin(),
        negocioId == null ? 0 : seguimientos.countByNegocioId(negocioId),
        s != null,
        s != null && s.isAlertas());
  }

  @Transactional
  public Estado activarPrueba(Long usuarioId, Long negocioId) {
    if (!demo) throw error("Las pruebas Plus están deshabilitadas. No se realizó ningún cobro");
    bloquearTitular(usuarioId, negocioId);
    String clave = clave(usuarioId, negocioId);
    if (membresias.findByClave(clave).isPresent())
      throw error("La prueba ya fue utilizada; no genera renovaciones ni cobros");
    membresias.saveAndFlush(new Membresia(clave, usuarioId, negocioId, ahora()));
    return estado(usuarioId, negocioId);
  }

  @Transactional
  public Estado cancelar(Long usuarioId, Long negocioId) {
    bloquearTitular(usuarioId, negocioId);
    membresias.findByClave(clave(usuarioId, negocioId)).ifPresent(m -> m.cancelar(ahora()));
    return estado(usuarioId, negocioId);
  }

  public void exigirClientePlus(Long id) {
    exigir("CLIENTE:" + id);
  }

  public void exigirNegocioPlus(Long id) {
    exigir("NEGOCIO:" + id);
  }

  private void exigir(String clave) {
    if (membresias.findByClave(clave).filter(m -> m.vigente(ahora())).isEmpty())
      throw error("Se necesita una membresía Plus vigente");
  }

  @Transactional
  public Estado seguir(Long usuarioId, Long negocioId, boolean alertas) {
    bloquearCliente(usuarioId);
    exigirNegocioPlus(negocioId);
    var s = seguimientos.findByClienteIdAndNegocioId(usuarioId, negocioId).orElse(null);
    if (s == null) seguimientos.saveAndFlush(new Seguimiento(usuarioId, negocioId, alertas));
    else s.cambiarAlertas(alertas);
    return estado(usuarioId, negocioId);
  }

  @Transactional
  public Estado dejarSeguir(Long usuarioId, Long negocioId) {
    bloquearCliente(usuarioId);
    seguimientos.findByClienteIdAndNegocioId(usuarioId, negocioId).ifPresent(seguimientos::delete);
    seguimientos.flush();
    return estado(usuarioId, negocioId);
  }

  @Transactional
  public Novedad publicar(Long usuarioId, Long negocioId, PublicacionRequest r) {
    bloquearTitular(usuarioId, negocioId);
    exigirNegocioPlus(negocioId);
    if (publicaciones.countByNegocioIdAndCreadaEnAfter(negocioId, ahora().minusHours(24)) >= 3)
      throw error("Puedes publicar hasta tres novedades en 24 horas");
    return resumir(
        publicaciones.save(
            new Publicacion(negocioId, r.titulo(), r.mensaje(), ahora(), r.vigenciaDias())));
  }

  @Transactional
  public void borrarPublicacion(Long usuarioId, Long negocioId, Long id) {
    bloquearTitular(usuarioId, negocioId);
    var p =
        publicaciones
            .findById(id)
            .filter(v -> v.getNegocioId().equals(negocioId))
            .orElseThrow(() -> error("Publicación no encontrada"));
    publicaciones.delete(p);
  }

  public Pagina<Novedad> novedades(Long usuarioId, int pagina) {
    var ids =
        seguimientos.findByClienteId(usuarioId).stream()
            .map(Seguimiento::getNegocioId)
            .filter(
                id ->
                    membresias
                        .findByClave("NEGOCIO:" + id)
                        .filter(m -> m.vigente(ahora()))
                        .isPresent())
            .toList();
    if (ids.isEmpty())
      return Pagina.de(
          org.springframework.data.domain.Page.<Novedad>empty(
              PageRequest.of(Math.max(0, pagina), 10)));
    return Pagina.de(
        publicaciones
            .findByNegocioIdInAndVenceEnAfter(ids, ahora(), pagina(pagina))
            .map(this::resumir));
  }

  public Pagina<Novedad> publicaciones(Long id, int pagina) {
    negocioService.buscarPorId(id);
    if (membresias.findByClave("NEGOCIO:" + id).filter(m -> m.vigente(ahora())).isEmpty())
      return Pagina.de(
          org.springframework.data.domain.Page.<Novedad>empty(
              PageRequest.of(Math.max(0, pagina), 10)));
    return Pagina.de(
        publicaciones
            .findByNegocioIdAndVenceEnAfter(id, ahora(), pagina(pagina))
            .map(this::resumir));
  }

  private PageRequest pagina(int n) {
    return PageRequest.of(
        Math.max(0, n), 10, org.springframework.data.domain.Sort.by("id").descending());
  }

  private Novedad resumir(Publicacion p) {
    return new Novedad(
        p.getId(),
        p.getNegocioId(),
        negocioService.buscarPorId(p.getNegocioId()).getNombre(),
        p.getTitulo(),
        p.getMensaje(),
        p.getCreadaEn(),
        p.getVenceEn());
  }

  private String clave(Long usuarioId, Long negocioId) {
    return negocioId == null ? "CLIENTE:" + usuarioId : "NEGOCIO:" + negocioId;
  }

  private Usuario usuario(Long id) {
    return usuarios
        .findById(id)
        .filter(Usuario::isActivo)
        .orElseThrow(() -> error("Usuario no disponible"));
  }

  private void bloquearCliente(Long id) {
    var u = usuarios.bloquearPorId(id).orElseThrow(() -> error("Usuario no disponible"));
    if (!u.isActivo() || u.getRol() != Rol.CLIENTE)
      throw error("Se necesita una cuenta de cliente");
  }

  private void bloquearTitular(Long usuarioId, Long negocioId) {
    if (negocioId == null) bloquearCliente(usuarioId);
    else {
      negocios.bloquearPorId(negocioId).orElseThrow(() -> error("Negocio no encontrado"));
      negocioService.obtenerDelPropietario(negocioId, usuarioId);
    }
  }

  private ResponseStatusException error(String m) {
    return new ResponseStatusException(HttpStatus.CONFLICT, m);
  }
}
