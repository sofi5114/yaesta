package pe.upn.yaesta.service;

import java.math.*;
import java.text.Normalizer;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.upn.yaesta.dto.TurnoResumen;
import pe.upn.yaesta.model.*;
import pe.upn.yaesta.repository.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlternativasService {
  private final TurnoRepository turnos;
  private final NegocioRepository negocios;
  private final ServicioRepository servicios;
  private final UsuarioRepository usuarios;
  private final NegocioService negocioService;
  private final MembresiaRepository membresias;
  private final TurnoService cola;
  private final EventoTurnoRepository historial;
  private static final List<EstadoTurno> ACTIVOS =
      List.of(EstadoTurno.EN_ESPERA, EstadoTurno.LLAMADO, EstadoTurno.ATENDIENDO);

  public record Configuracion(
      Double latitud, Double longitud, int descuento, int cupos, LocalDateTime actualizadaEn) {}

  public record Alternativa(
      Long negocioId,
      Long servicioId,
      String negocio,
      String servicio,
      double distanciaKm,
      Long esperaMinutos,
      int duracionMinutos,
      BigDecimal precio,
      BigDecimal precioFinal,
      int descuento,
      LocalDateTime actualizadaEn) {}

  @Transactional
  public Configuracion configurar(
      Long usuarioId, Long negocioId, double lat, double lon, int descuento, int cupos) {
    negocios.bloquearPorId(negocioId).orElseThrow(() -> error("Negocio no encontrado"));
    var n = negocioService.obtenerDelPropietario(negocioId, usuarioId);
    if (descuento > 0) exigirPlus(negocioId);
    n.ubicar(lat, lon, descuento, cupos);
    n.actualizarDisponibilidad(PlusService.ahora());
    return config(n);
  }

  public Configuracion configuracion(Long usuarioId, Long negocioId) {
    return config(negocioService.obtenerDelPropietario(negocioId, usuarioId));
  }

  private Configuracion config(Negocio n) {
    return new Configuracion(
        n.getLatitud(),
        n.getLongitud(),
        n.getDescuentoDerivacion(),
        n.getCuposDerivacion(),
        n.getDisponibilidadActualizada());
  }

  public List<Alternativa> buscar(Long usuarioId, Long turnoId, double lat, double lon) {
    var origen = propio(usuarioId, turnoId);
    return negocios.findAll().stream()
        .filter(n -> !n.getId().equals(origen.getNegocio().getId()) && vigente(n))
        .filter(n -> distancia(lat, lon, n.getLatitud(), n.getLongitud()) <= 10)
        .flatMap(
            n ->
                servicios
                    .findByNegocioIdAndActivoTrue(
                        n.getId(), org.springframework.data.domain.Pageable.unpaged())
                    .stream()
                    .filter(
                        s ->
                            normalizar(s.getNombre())
                                .equals(normalizar(origen.getServicio().getNombre())))
                    .map(
                        s ->
                            alternativa(
                                n, s, lat, lon, cola.resumir(origen).demoraSignificativa())))
        .filter(a -> a.esperaMinutos() != null)
        .sorted(
            Comparator.comparingDouble(
                a -> a.esperaMinutos() + a.distanciaKm() * 4 + a.duracionMinutos()))
        .limit(10)
        .toList();
  }

  @Transactional
  public TurnoResumen cambiar(
      Long usuarioId, Long turnoId, Long servicioId, BigDecimal precioAceptado, int minutosTraslado) {
    var inicial = propio(usuarioId, turnoId);
    var servicioInicial =
        servicios.findById(servicioId).orElseThrow(() -> error("Servicio no encontrado"));
    Long origenId = inicial.getNegocio().getId(), destinoId = servicioInicial.getNegocio().getId();
    if (origenId.equals(destinoId)) throw error("Selecciona otro negocio");
    // All transfers lock both queues in ascending order to avoid deadlocks.
    negocios.bloquearPorId(Math.min(origenId, destinoId)).orElseThrow();
    negocios.bloquearPorId(Math.max(origenId, destinoId)).orElseThrow();
    em.refresh(inicial);
    em.refresh(servicioInicial);
    em.refresh(servicioInicial.getNegocio());
    var origen = propio(usuarioId, turnoId);
    var n = servicioInicial.getNegocio();
    if (!vigente(n) || !servicioInicial.isActivo())
      throw error("La disponibilidad cambió; vuelve a consultar alternativas");
    if (!normalizar(origen.getServicio().getNombre())
        .equals(normalizar(servicioInicial.getNombre()))) throw error("El servicio no coincide");
    if (turnos.existsByNegocioIdAndClienteIdAndEstadoIn(destinoId, usuarioId, ACTIVOS))
      throw error("Ya tienes un turno en el negocio de destino");
    if(turnos.colaOrdenada(destinoId,ACTIVOS).size()>=100) throw error("El negocio alcanzó su capacidad de cola; consulta otra alternativa");
    if (espera(destinoId) == null)
      throw error("El negocio debe actualizar su demora antes de recibir derivaciones");
    int descuento = cola.resumir(origen).demoraSignificativa() ? descuento(n) : 0;
    BigDecimal finalPrecio = precio(servicioInicial, descuento);
    if (finalPrecio.compareTo(precioAceptado) != 0)
      throw error("El precio o el beneficio cambió; revisa y confirma otra vez");
    var nuevo =
        new Turno(
            servicioInicial,
            usuarios.findById(usuarioId).orElseThrow(),
            null,
            OrigenTurno.BUSQUEDA,
            PlusService.ahora());
    nuevo.registrarDerivacion(turnoId, descuento);
    nuevo.protegerTraslado(PlusService.ahora().plusMinutes(minutosTraslado));
    turnos.saveAndFlush(nuevo);
    nuevo.registrarEstimacionInicial(cola.resumir(nuevo).esperaEstimadaMinutos());
    if (descuento > 0) n.consumirCupoDerivacion();
    origen.cancelar(PlusService.ahora());
    historial.save(
        new EventoTurno(
            origen.getId(),
            usuarioId,
            "DERIVACION",
            "Cambio confirmado al turno #" + nuevo.getId(),
            PlusService.ahora()));
    historial.save(
        new EventoTurno(
            nuevo.getId(),
            usuarioId,
            "DERIVACION_RECIBIDA",
            "Origen #"
                + turnoId
                + ". Precio confirmado S/"
                + finalPrecio
                + ". Descuento "
                + descuento
                + "%",
            PlusService.ahora()));
    return cola.resumir(nuevo);
  }

  private final jakarta.persistence.EntityManager em;

  private Turno propio(Long id, Long turnoId) {
    var t =
        turnos.findByIdAndClienteId(turnoId, id).orElseThrow(() -> error("Turno no encontrado"));
    if (t.getEstado() != EstadoTurno.EN_ESPERA || t.getLlegadaEn() != null)
      throw error("Solo puedes cambiar un turno remoto en espera, antes de confirmar llegada");
    return t;
  }

  private boolean vigente(Negocio n) {
    return n.isAceptaTurnos()
        && n.getLatitud() != null
        && n.getLongitud() != null
        && n.getDisponibilidadActualizada() != null
        && n.getDisponibilidadActualizada().isAfter(PlusService.ahora().minusMinutes(10));
  }

  private int descuento(Negocio n) {
    return n.getCuposDerivacion() > 0
            && membresias
                .findByClave("NEGOCIO:" + n.getId())
                .filter(m -> m.vigente(PlusService.ahora()))
                .isPresent()
        ? n.getDescuentoDerivacion()
        : 0;
  }

  private void exigirPlus(Long id) {
    if (membresias
        .findByClave("NEGOCIO:" + id)
        .filter(m -> m.vigente(PlusService.ahora()))
        .isEmpty()) throw error("La promoción para derivaciones necesita Negocio Plus");
  }

  private Long espera(Long id) {
    long total = 0;
    for (var t : turnos.colaOrdenada(id, ACTIVOS)) {
      var m = cola.minutosRestantes(t, PlusService.ahora());
      if (m == null) return null;
      total += m;
    }
    return total;
  }

  private BigDecimal precio(Servicio s, int descuento) {
    return s.getPrecio()
        .multiply(BigDecimal.valueOf(100 - descuento))
        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
  }

  private Alternativa alternativa(Negocio n, Servicio s, double lat, double lon, boolean demora) {
    int d = demora ? descuento(n) : 0;
    return new Alternativa(
        n.getId(),
        s.getId(),
        n.getNombre(),
        s.getNombre(),
        Math.round(distancia(lat, lon, n.getLatitud(), n.getLongitud()) * 10) / 10.0,
        espera(n.getId()),
        s.getDuracionMinutos(),
        s.getPrecio(),
        precio(s, d),
        d,
        n.getDisponibilidadActualizada());
  }

  static double distancia(double a, double b, double c, double d) {
    double x = Math.sin(Math.toRadians(c - a) / 2), y = Math.sin(Math.toRadians(d - b) / 2);
    double h = x * x + Math.cos(Math.toRadians(a)) * Math.cos(Math.toRadians(c)) * y * y;
    return 6371 * 2 * Math.asin(Math.sqrt(Math.min(1, h)));
  }

  private String normalizar(String s) {
    return Normalizer.normalize(s, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .trim();
  }

  private ResponseStatusException error(String m) {
    return new ResponseStatusException(HttpStatus.CONFLICT, m);
  }
}
