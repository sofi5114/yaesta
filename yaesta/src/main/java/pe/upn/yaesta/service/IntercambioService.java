package pe.upn.yaesta.service;

import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.upn.yaesta.model.*;
import pe.upn.yaesta.repository.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IntercambioService {
  private final TurnoRepository turnos;
  private final NegocioRepository negocios;
  private final IntercambioRepository intercambios;
  private final PlusService plus;
  private final EventoTurnoRepository historial;
  private final jakarta.persistence.EntityManager em;

  public record Resumen(
      Long id,
      Long origenId,
      Long destinoId,
      boolean recibido,
      String estado,
      LocalDateTime venceEn) {}

  public List<Resumen> listar(Long usuarioId) {
    return intercambios
        .findTop50ByReceptorIdOrSolicitanteIdOrderByIdDesc(usuarioId, usuarioId)
        .stream()
        .map(i -> resumir(i, usuarioId))
        .toList();
  }

  @Transactional
  public Resumen proponer(Long usuarioId, Long origenId, Long destinoId) {
    var origen =
        turnos
            .findByIdAndClienteId(origenId, usuarioId)
            .orElseThrow(() -> error("Turno no encontrado"));
    negocios
        .bloquearPorId(origen.getNegocio().getId())
        .orElseThrow(() -> error("Negocio no encontrado"));
    em.refresh(origen);
    var destino =
        turnos
            .findByIdAndNegocioId(destinoId, origen.getNegocio().getId())
            .orElseThrow(() -> error("No hay un turno compatible con ese número"));
    comprobar(origen, destino);
    if (intercambios.existsByOrigenIdAndEstadoAndVenceEnAfter(
        origenId, "PENDIENTE", PlusService.ahora()))
      throw error("Ya tienes una propuesta pendiente; cancélala o espera a que venza");
    return resumir(
        intercambios.save(new Intercambio(origen, destino, PlusService.ahora())), usuarioId);
  }

  @Transactional
  public Resumen resolver(Long usuarioId, Long id, boolean aceptar) {
    var inicial = intercambios.findById(id).orElseThrow(() -> error("Propuesta no encontrada"));
    negocios
        .bloquearPorId(inicial.getNegocioId())
        .orElseThrow(() -> error("Negocio no encontrado"));
    em.refresh(inicial);
    // Reload after acquiring the shared queue lock; all changes use the same lock.
    var i = intercambios.findById(id).orElseThrow();
    if (!i.getReceptorId().equals(usuarioId) && !i.getSolicitanteId().equals(usuarioId))
      throw error("Esta propuesta no te pertenece");
    if (!i.getEstado().equals("PENDIENTE")) throw error("La propuesta ya fue resuelta");
    if (!PlusService.ahora().isBefore(i.getVenceEn())) throw error("La propuesta venció");
    if (aceptar) {
      if (!i.getReceptorId().equals(usuarioId)) throw error("Solo la otra persona puede aceptar");
      var origen = turnos.findById(i.getOrigenId()).orElseThrow();
      var destino = turnos.findById(i.getDestinoId()).orElseThrow();
      comprobar(origen, destino);
      if (origen.orden() != i.getOrdenOrigen() || destino.orden() != i.getOrdenDestino())
        throw error("Los turnos cambiaron; solicita un nuevo intercambio");
      origen.intercambiarOrden(i.getOrdenDestino());
      destino.intercambiarOrden(i.getOrdenOrigen());
      // A voluntary change must not be presented as a delay caused by the business.
      origen.registrarEstimacionInicial(null);
      destino.registrarEstimacionInicial(null);
      i.resolver("ACEPTADO");
      historial.save(
          new EventoTurno(
              origen.getId(),
              usuarioId,
              "INTERCAMBIO",
              "Intercambio aceptado con turno #" + destino.getId(),
              PlusService.ahora()));
      historial.save(
          new EventoTurno(
              destino.getId(),
              usuarioId,
              "INTERCAMBIO",
              "Intercambio aceptado con turno #" + origen.getId(),
              PlusService.ahora()));
    } else i.resolver(i.getReceptorId().equals(usuarioId) ? "RECHAZADO" : "CANCELADO");
    return resumir(i, usuarioId);
  }

  private void comprobar(Turno a, Turno b) {
    if (a.getId().equals(b.getId())
        || a.getCliente() == null
        || b.getCliente() == null
        || a.getCliente().getId().equals(b.getCliente().getId()))
      throw error("Se requieren dos clientes diferentes");
    if (a.getEstado() != EstadoTurno.EN_ESPERA
        || b.getEstado() != EstadoTurno.EN_ESPERA
        || a.getLlegadaEn() != null
        || b.getLlegadaEn() != null)
      throw error("Solo se intercambian turnos remotos en espera, antes de confirmar llegada");
    if (!a.getServicio().getId().equals(b.getServicio().getId()) || a.duracion() != b.duracion())
      throw error("Los turnos deben tener el mismo servicio y duración");
    if (!a.disponibleParaLlamado(PlusService.ahora()) || !b.disponibleParaLlamado(PlusService.ahora()))
      throw error("No puedes intercambiar un turno durante su traslado protegido");
    if (a.cambios() > 0 || b.cambios() > 0) throw error("Cada turno admite un solo intercambio");
    plus.exigirClientePlus(a.getCliente().getId());
    plus.exigirClientePlus(b.getCliente().getId());
  }

  private Resumen resumir(Intercambio i, Long usuarioId) {
    String estado = i.getEstado();
    if (estado.equals("PENDIENTE") && !PlusService.ahora().isBefore(i.getVenceEn()))
      estado = "VENCIDO";
    return new Resumen(
        i.getId(),
        i.getOrigenId(),
        i.getDestinoId(),
        i.getReceptorId().equals(usuarioId),
        estado,
        i.getVenceEn());
  }

  private ResponseStatusException error(String m) {
    return new ResponseStatusException(HttpStatus.CONFLICT, m);
  }
}
