package pe.upn.yaesta.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
public class TurnoService {

  private static final List<EstadoTurno> ACTIVOS =
      List.of(EstadoTurno.EN_ESPERA, EstadoTurno.LLAMADO, EstadoTurno.ATENDIENDO);

  private static final List<EstadoTurno> EN_ATENCION =
      List.of(EstadoTurno.LLAMADO, EstadoTurno.ATENDIENDO);

  private final TurnoRepository turnos;
  private final NegocioRepository negocios;
  private final ServicioRepository servicios;
  private final UsuarioRepository usuarios;
  private final NegocioService negocioService;
  private final RecordatorioService recordatorioService;
  private final ApplicationEventPublisher eventos;
  private final EventoTurnoRepository historial;

  public Pagina<TurnoResumen> misTurnos(Long clienteId, int pagina, int tamano) {

    var pedido = crearPagina(pagina, tamano, Sort.by("id").descending());

    return Pagina.de(turnos.findByClienteId(clienteId, pedido).map(this::resumir));
  }

  public TurnoResumen miTurno(Long clienteId, Long turnoId) {
    Turno turno =
        turnos
            .findByIdAndClienteId(turnoId, clienteId)
            .orElseThrow(() -> noEncontrado("Turno no encontrado"));

    return resumir(turno);
  }

  public Pagina<TurnoResumen> cola(Long negocioId, Long propietarioId, int pagina, int tamano) {

    negocioService.obtenerDelPropietario(negocioId, propietarioId);

    var pedido = crearPagina(pagina, tamano, Sort.by("id").ascending());

    var lista = turnos.colaOrdenada(negocioId, ACTIVOS);
    int desde = (int) Math.min(pedido.getOffset(), lista.size());
    int hasta = Math.min(desde + pedido.getPageSize(), lista.size());
    return Pagina.de(
        new org.springframework.data.domain.PageImpl<>(
            lista.subList(desde, hasta).stream().map(this::resumir).toList(),
            pedido,
            lista.size()));
  }

  public ConfiguracionColaResumen configuracion(Long negocioId, Long propietarioId) {

    Negocio negocio = negocioService.obtenerDelPropietario(negocioId, propietarioId);

    return ConfiguracionColaResumen.de(negocio);
  }

  @Transactional
  public ConfiguracionColaResumen configurar(
      Long negocioId, Long propietarioId, ConfiguracionColaRequest request) {

    Negocio negocio = bloquearDelPropietario(negocioId, propietarioId);

    negocio.cambiarRecepcionTurnos(request.aceptaTurnos());
    negocio.cambiarTolerancia(request.toleranciaMinutos());

    return ConfiguracionColaResumen.de(negocio);
  }

  @Transactional
  public TurnoResumen solicitar(Long negocioId, Long clienteId, TurnoRequest request) {

    Negocio negocio = bloquearNegocio(negocioId);
    comprobarRecepcion(negocio);

    if (turnos.existsByNegocioIdAndClienteIdAndEstadoIn(negocioId, clienteId, ACTIVOS)) {

      throw conflicto("Ya tienes un turno activo en este negocio");
    }

    Servicio servicio = obtenerServicio(negocioId, request.servicioId());

    Usuario cliente =
        usuarios.findById(clienteId).orElseThrow(() -> noEncontrado("Cliente no encontrado"));

    Turno turno = new Turno(servicio, cliente, null, OrigenTurno.BUSQUEDA, ahora());

    turnos.saveAndFlush(turno);
    turno.registrarEstimacionInicial(resumir(turno).esperaEstimadaMinutos());
    return resumir(turno);
  }

  @Transactional
  public TurnoResumen registrarPresencial(
      Long negocioId, Long propietarioId, TurnoPresencialRequest request) {

    Negocio negocio = bloquearDelPropietario(negocioId, propietarioId);

    comprobarRecepcion(negocio);

    Servicio servicio = obtenerServicio(negocioId, request.servicioId());

    Turno turno =
        new Turno(servicio, null, request.nombreCliente().trim(), OrigenTurno.PRESENCIAL, ahora());

    turnos.saveAndFlush(turno);
    turno.registrarEstimacionInicial(resumir(turno).esperaEstimadaMinutos());
    return resumir(turno);
  }

  @Transactional
  public TurnoResumen cancelar(Long negocioId, Long turnoId, Long clienteId) {

    bloquearNegocio(negocioId);

    Turno turno =
        turnos
            .findByIdAndClienteId(turnoId, clienteId)
            .filter(t -> t.getNegocio().getId().equals(negocioId))
            .orElseThrow(() -> noEncontrado("Turno no encontrado"));

    if (turno.getEstado() != EstadoTurno.EN_ESPERA && turno.getEstado() != EstadoTurno.LLAMADO) {

      throw conflicto("Solo puedes cancelar un turno en espera o llamado");
    }

    turno.cancelar(ahora());
    auditar(turno, clienteId, "CANCELADO", "Cancelado por el cliente");

    return resumir(turno);
  }

  @Transactional
  public TurnoResumen llamarSiguiente(Long negocioId, Long propietarioId) {

    bloquearDelPropietario(negocioId, propietarioId);

    if (turnos.existsByNegocioIdAndEstadoIn(negocioId, EN_ATENCION)) {

      throw conflicto("Primero termina de gestionar el turno actual");
    }

    Turno turno =
        turnos.colaOrdenada(negocioId, List.of(EstadoTurno.EN_ESPERA)).stream()
            .filter(t -> t.disponibleParaLlamado(ahora()))
            .findFirst()
            .orElseThrow(() -> conflicto("No hay clientes listos para llamar; puede haber clientes en traslado"));

    turno.llamar(ahora());
    auditar(turno, propietarioId, "LLAMADO", "Se llamó al cliente");

    Usuario cliente = turno.getCliente();

    if (cliente != null && cliente.isActivo()) {
      eventos.publishEvent(
          new TurnoLlamado(
              turno.getId(),
              cliente.getCorreo(),
              cliente.getNombre(),
              turno.getNegocio().getNombre(),
              turno.getLlamadoEn().plusMinutes(turno.getToleranciaMinutos())));
    }

    return resumir(turno);
  }

  @Transactional
  public TurnoResumen registrarLlegada(Long negocioId, Long turnoId, Long propietarioId) {

    Turno turno = obtenerParaGestion(negocioId, turnoId, propietarioId);

    if (turno.getEstado() != EstadoTurno.EN_ESPERA && turno.getEstado() != EstadoTurno.LLAMADO) {

      throw conflicto("Solo puedes registrar la llegada de un turno en espera o llamado");
    }

    LocalDateTime fecha = ahora();

    if (turno.getLlegadaEn() == null) {
      if (turno.getEstado() == EstadoTurno.LLAMADO && plazoVencido(turno, fecha)) {

        throw conflicto("Venció la tolerancia. Corresponde marcar ausencia");
      }

      turno.registrarLlegada(fecha);
      auditar(turno, propietarioId, "LLEGADA", "Presencia confirmada por el negocio");
    }

    return resumir(turno);
  }

  @Transactional
  public TurnoResumen iniciar(Long negocioId, Long turnoId, Long propietarioId) {

    Turno turno = obtenerParaGestion(negocioId, turnoId, propietarioId);

    exigirEstado(turno, EstadoTurno.LLAMADO);

    if (turno.getLlegadaEn() == null) {
      throw conflicto("Primero confirma que el cliente llegó");
    }

    turno.iniciar(ahora());
    auditar(turno, propietarioId, "INICIO", "Atención iniciada");

    return resumir(turno);
  }

  @Transactional
  public TurnoResumen completar(Long negocioId, Long turnoId, Long propietarioId) {

    Turno turno = obtenerParaGestion(negocioId, turnoId, propietarioId);

    exigirEstado(turno, EstadoTurno.ATENDIENDO);
    turno.completar(ahora());
    auditar(turno, propietarioId, "ATENDIDO", "Atención completada");

    recordatorioService.generar(turno);

    return resumir(turno);
  }

  @Transactional
  public TurnoResumen marcarAusente(Long negocioId, Long turnoId, Long propietarioId) {

    Turno turno = obtenerParaGestion(negocioId, turnoId, propietarioId);

    exigirEstado(turno, EstadoTurno.LLAMADO);

    if (turno.getLlegadaEn() != null) {
      throw conflicto("El cliente ya tiene su llegada registrada");
    }

    LocalDateTime fecha = ahora();

    if (!plazoVencido(turno, fecha)) {
      throw conflicto("Todavía no termina el plazo de tolerancia");
    }

    turno.marcarAusente(fecha);
    auditar(turno, propietarioId, "AUSENTE", "Ausencia confirmada por el negocio");

    return resumir(turno);
  }

  @Transactional
  public TurnoResumen actualizarDemora(
      Long negocioId, Long turnoId, Long propietarioId, int minutos) {
    Turno turno = obtenerParaGestion(negocioId, turnoId, propietarioId);
    exigirEstado(turno, EstadoTurno.ATENDIENDO);
    turno.actualizarFinEstimado(ahora().plusMinutes(minutos));
    auditar(
        turno, propietarioId, "DEMORA", "Tiempo restante actualizado a " + minutos + " minutos");
    return resumir(turno);
  }

  @Transactional
  public void vencerAusencia(Long negocioId, Long turnoId, LocalDateTime fecha) {
    bloquearNegocio(negocioId);
    turnos
        .findByIdAndNegocioId(turnoId, negocioId)
        .ifPresent(
            turno -> {
              if (turno.getEstado() == EstadoTurno.LLAMADO
                  && turno.getLlegadaEn() == null
                  && plazoVencido(turno, fecha)) {
                turno.marcarAusente(fecha);
                auditar(
                    turno,
                    null,
                    "VENCIMIENTO_AUTOMATICO",
                    "Venció la tolerancia sin llegada confirmada");
              }
            });
  }

  public record EventoResumen(String accion, String detalle, LocalDateTime fecha) {}

  public List<EventoResumen> historialCliente(Long clienteId, Long turnoId) {
    turnos
        .findByIdAndClienteId(turnoId, clienteId)
        .orElseThrow(() -> noEncontrado("Turno no encontrado"));
    return historial.findByTurnoIdOrderByIdAsc(turnoId).stream()
        .map(e -> new EventoResumen(e.getAccion(), e.getDetalle(), e.getFecha()))
        .toList();
  }

  @Transactional
  public TurnoResumen cancelarPorNegocio(
      Long negocioId, Long turnoId, Long propietarioId, String motivo) {
    var turno = obtenerParaGestion(negocioId, turnoId, propietarioId);
    if (turno.getEstado() != EstadoTurno.EN_ESPERA && turno.getEstado() != EstadoTurno.LLAMADO)
      throw conflicto("Solo puedes cancelar turnos en espera o llamados");
    turno.cancelar(ahora());
    auditar(turno, propietarioId, "CANCELADO_POR_NEGOCIO", motivo.trim());
    return resumir(turno);
  }

  private void auditar(Turno turno, Long actorId, String accion, String detalle) {
    historial.save(new EventoTurno(turno.getId(), actorId, accion, detalle, ahora()));
  }

  private Turno obtenerParaGestion(Long negocioId, Long turnoId, Long propietarioId) {

    bloquearDelPropietario(negocioId, propietarioId);

    return turnos
        .findByIdAndNegocioId(turnoId, negocioId)
        .orElseThrow(() -> noEncontrado("Turno no encontrado"));
  }

  private Negocio bloquearNegocio(Long negocioId) {
    return negocios
        .bloquearPorId(negocioId)
        .orElseThrow(() -> noEncontrado("Negocio no encontrado"));
  }

  private Negocio bloquearDelPropietario(Long negocioId, Long propietarioId) {

    bloquearNegocio(negocioId);

    Negocio negocio = negocioService.obtenerDelPropietario(negocioId, propietarioId);
    negocio.actualizarDisponibilidad(ahora());
    return negocio;
  }

  private Servicio obtenerServicio(Long negocioId, Long servicioId) {

    return servicios
        .findByIdAndNegocioIdAndActivoTrue(servicioId, negocioId)
        .orElseThrow(() -> noEncontrado("Servicio no encontrado en este negocio"));
  }

  private void comprobarRecepcion(Negocio negocio) {
    if (turnos.colaOrdenada(negocio.getId(),ACTIVOS).size() >= 100)
      throw conflicto("El negocio alcanzó el límite de 100 turnos activos; consulta otra alternativa");
    if (!negocio.isAceptaTurnos()) {
      throw conflicto("El negocio no está recibiendo nuevos turnos");
    }
  }

  private void exigirEstado(Turno turno, EstadoTurno esperado) {

    if (turno.getEstado() != esperado) {
      throw conflicto("El turno debe estar en estado " + esperado);
    }
  }

  private boolean plazoVencido(Turno turno, LocalDateTime fecha) {

    LocalDateTime limite = turno.getLlamadoEn().plusMinutes(turno.getToleranciaMinutos());

    return !fecha.isBefore(limite);
  }

  TurnoResumen resumir(Turno turno) {
    Long posicion = null;
    Long espera = null;

    if (turno.getEstado() == EstadoTurno.EN_ESPERA) {
      var activos = turnos.colaOrdenada(turno.getNegocio().getId(), ACTIVOS);
      LocalDateTime fecha = ahora();
      var plan = planificar(activos, fecha);
      espera = plan.esperas().get(turno.getId());
      int indice = plan.orden().indexOf(turno.getId());
      posicion = indice < 0 ? null : (long) indice + 1;
    } else if (EN_ATENCION.contains(turno.getEstado())) {
      posicion = 0L;
      espera = 0L;
    }

    return TurnoResumen.de(turno, posicion, espera);
  }

  record Plan(java.util.Map<Long,Long> esperas, java.util.List<Long> orden) {}

  private Plan planificar(List<Turno> activos, LocalDateTime fecha) {
    var esperas = new java.util.HashMap<Long,Long>();
    var orden = new java.util.ArrayList<Long>();
    LocalDateTime libre = fecha;
    for (var t : activos) {
      if (EN_ATENCION.contains(t.getEstado())) {
        Long restante = minutosRestantes(t,fecha);
        if (restante == null) {
          return new Plan(esperas, activos.stream().filter(x -> x.getEstado()==EstadoTurno.EN_ESPERA).map(Turno::getId).toList());
        }
        libre = libre.plusMinutes(restante);
      }
    }
    var pendientes = new java.util.ArrayList<>(activos.stream().filter(t -> t.getEstado()==EstadoTurno.EN_ESPERA).toList());
    while (!pendientes.isEmpty()) {
      Turno elegido = null;
      for (var t : pendientes) { if (t.disponibleParaLlamado(libre)) { elegido=t;break; } }
      if (elegido == null) {
        libre = pendientes.stream().map(Turno::getNoLlamarAntesDe).filter(java.util.Objects::nonNull).min(LocalDateTime::compareTo).orElse(libre);
        continue;
      }
      long segundos = Math.max(0,Duration.between(fecha,libre).getSeconds());
      esperas.put(elegido.getId(),(segundos+59)/60);
      orden.add(elegido.getId());
      libre=libre.plusMinutes(elegido.duracion());
      pendientes.remove(elegido);
    }
    return new Plan(esperas,orden);
  }

  Long minutosRestantes(Turno turno, LocalDateTime fecha) {

    long duracion = turno.duracion();

    if (turno.getEstado() == EstadoTurno.ATENDIENDO) {
      LocalDateTime fin =
          turno.getFinEstimado() == null
              ? turno.getInicioEn().plusMinutes(duracion)
              : turno.getFinEstimado();
      long restantes = Duration.between(fecha, fin).getSeconds();

      if (restantes <= 0) {
        return null;
      }

      return (restantes + 59) / 60;
    }

    if (turno.getEstado() == EstadoTurno.LLAMADO && turno.getLlegadaEn() == null) {

      LocalDateTime limite = turno.getLlamadoEn().plusMinutes(turno.getToleranciaMinutos());

      long segundos = Math.max(Duration.between(fecha, limite).getSeconds(), 0);

      return duracion + (segundos + 59) / 60;
    }

    return duracion;
  }

  private PageRequest crearPagina(int pagina, int tamano, Sort orden) {

    int numero = Math.max(pagina, 0);
    int cantidad = Math.min(Math.max(tamano, 1), 50);

    return PageRequest.of(numero, cantidad, orden);
  }

  private LocalDateTime ahora() {
    return LocalDateTime.now(ZoneId.of("America/Lima"));
  }

  private ResponseStatusException noEncontrado(String mensaje) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, mensaje);
  }

  private ResponseStatusException conflicto(String mensaje) {
    return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
  }
}
