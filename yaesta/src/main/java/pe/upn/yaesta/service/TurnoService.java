package pe.upn.yaesta.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

import pe.upn.yaesta.dto.*;
import pe.upn.yaesta.model.*;
import pe.upn.yaesta.repository.*;
import org.springframework.context.ApplicationEventPublisher;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TurnoService {

    private static final List<EstadoTurno> ACTIVOS = List.of(
        EstadoTurno.EN_ESPERA,
        EstadoTurno.LLAMADO,
        EstadoTurno.ATENDIENDO
    );

    private static final List<EstadoTurno> EN_ATENCION = List.of(
        EstadoTurno.LLAMADO,
        EstadoTurno.ATENDIENDO
    );

    private final TurnoRepository turnos;
    private final NegocioRepository negocios;
    private final ServicioRepository servicios;
    private final UsuarioRepository usuarios;
    private final NegocioService negocioService;
    private final RecordatorioService recordatorioService;
    private final ApplicationEventPublisher eventos;

    public Pagina<TurnoResumen> misTurnos(
            Long clienteId, int pagina, int tamano) {

        var pedido = crearPagina(
            pagina, tamano, Sort.by("id").descending()
        );

        return Pagina.de(
            turnos.findByClienteId(clienteId, pedido)
                .map(this::resumir)
        );
    }

    public TurnoResumen miTurno(Long clienteId, Long turnoId) {
        Turno turno = turnos
            .findByIdAndClienteId(turnoId, clienteId)
            .orElseThrow(() -> noEncontrado("Turno no encontrado"));

        return resumir(turno);
    }

    public Pagina<TurnoResumen> cola(
            Long negocioId, Long propietarioId,
            int pagina, int tamano) {

        negocioService.obtenerDelPropietario(
            negocioId, propietarioId
        );

        var pedido = crearPagina(
            pagina, tamano, Sort.by("id").ascending()
        );

        return Pagina.de(
            turnos.findByNegocioIdAndEstadoIn(
                negocioId, ACTIVOS, pedido
            ).map(this::resumir)
        );
    }

    public ConfiguracionColaResumen configuracion(
            Long negocioId, Long propietarioId) {

        Negocio negocio = negocioService.obtenerDelPropietario(
            negocioId, propietarioId
        );

        return ConfiguracionColaResumen.de(negocio);
    }

    @Transactional
    public ConfiguracionColaResumen configurar(
            Long negocioId, Long propietarioId,
            ConfiguracionColaRequest request) {

        Negocio negocio = bloquearDelPropietario(
            negocioId, propietarioId
        );

        negocio.cambiarRecepcionTurnos(request.aceptaTurnos());
        negocio.cambiarTolerancia(request.toleranciaMinutos());

        return ConfiguracionColaResumen.de(negocio);
    }

    @Transactional
    public TurnoResumen solicitar(
            Long negocioId, Long clienteId,
            TurnoRequest request) {

        Negocio negocio = bloquearNegocio(negocioId);
        comprobarRecepcion(negocio);

        if (turnos.existsByNegocioIdAndClienteIdAndEstadoIn(
                negocioId, clienteId, ACTIVOS)) {

            throw conflicto(
                "Ya tienes un turno activo en este negocio"
            );
        }

        Servicio servicio = obtenerServicio(
            negocioId, request.servicioId()
        );

        Usuario cliente = usuarios.findById(clienteId)
            .orElseThrow(() -> noEncontrado("Cliente no encontrado"));

        Turno turno = new Turno(
            servicio,
            cliente,
            null,
            OrigenTurno.BUSQUEDA,
            ahora()
        );

        return resumir(turnos.save(turno));
    }

    @Transactional
    public TurnoResumen registrarPresencial(
            Long negocioId, Long propietarioId,
            TurnoPresencialRequest request) {

        Negocio negocio = bloquearDelPropietario(
            negocioId, propietarioId
        );

        comprobarRecepcion(negocio);

        Servicio servicio = obtenerServicio(
            negocioId, request.servicioId()
        );

        Turno turno = new Turno(
            servicio,
            null,
            request.nombreCliente().trim(),
            OrigenTurno.PRESENCIAL,
            ahora()
        );

        return resumir(turnos.save(turno));
    }

    @Transactional
    public TurnoResumen cancelar(
            Long negocioId, Long turnoId, Long clienteId) {

        bloquearNegocio(negocioId);

        Turno turno = turnos
            .findByIdAndClienteId(turnoId, clienteId)
            .filter(t -> t.getNegocio().getId().equals(negocioId))
            .orElseThrow(() -> noEncontrado("Turno no encontrado"));

        if (turno.getEstado() != EstadoTurno.EN_ESPERA
                && turno.getEstado() != EstadoTurno.LLAMADO) {

            throw conflicto(
                "Solo puedes cancelar un turno en espera o llamado"
            );
        }

        turno.cancelar(ahora());

        return resumir(turno);
    }

   @Transactional
public TurnoResumen llamarSiguiente(
        Long negocioId, Long propietarioId) {

    bloquearDelPropietario(negocioId, propietarioId);

    if (turnos.existsByNegocioIdAndEstadoIn(
            negocioId, EN_ATENCION)) {

        throw conflicto(
            "Primero termina de gestionar el turno actual"
        );
    }

    Turno turno = turnos
        .findFirstByNegocioIdAndEstadoOrderByIdAsc(
            negocioId, EstadoTurno.EN_ESPERA
        )
        .orElseThrow(() -> conflicto("No hay turnos en espera"));

    turno.llamar(ahora());

    Usuario cliente = turno.getCliente();

    if (cliente != null && cliente.isActivo()) {
        eventos.publishEvent(new TurnoLlamado(
            turno.getId(),
            cliente.getCorreo(),
            cliente.getNombre(),
            turno.getNegocio().getNombre(),
            turno.getLlamadoEn()
                .plusMinutes(turno.getToleranciaMinutos())
        ));
    }

    return resumir(turno);
}

    @Transactional
    public TurnoResumen registrarLlegada(
            Long negocioId, Long turnoId, Long propietarioId) {

        Turno turno = obtenerParaGestion(
            negocioId, turnoId, propietarioId
        );

        if (turno.getEstado() != EstadoTurno.EN_ESPERA
                && turno.getEstado() != EstadoTurno.LLAMADO) {

            throw conflicto(
                "Solo puedes registrar la llegada de un turno en espera o llamado"
            );
        }

        LocalDateTime fecha = ahora();

        if (turno.getLlegadaEn() == null) {
            if (turno.getEstado() == EstadoTurno.LLAMADO
                    && plazoVencido(turno, fecha)) {

                throw conflicto(
                    "Venció la tolerancia. Corresponde marcar ausencia"
                );
            }

            turno.registrarLlegada(fecha);
        }

        return resumir(turno);
    }

    @Transactional
    public TurnoResumen iniciar(
            Long negocioId, Long turnoId, Long propietarioId) {

        Turno turno = obtenerParaGestion(
            negocioId, turnoId, propietarioId
        );

        exigirEstado(turno, EstadoTurno.LLAMADO);

        if (turno.getLlegadaEn() == null) {
            throw conflicto(
                "Primero confirma que el cliente llegó"
            );
        }

        turno.iniciar(ahora());

        return resumir(turno);
    }

    @Transactional
    public TurnoResumen completar(
            Long negocioId, Long turnoId, Long propietarioId) {

        Turno turno = obtenerParaGestion(
            negocioId, turnoId, propietarioId
        );

        exigirEstado(turno, EstadoTurno.ATENDIENDO);
        turno.completar(ahora());

        recordatorioService.generar(turno);

        return resumir(turno);
    }

    @Transactional
    public TurnoResumen marcarAusente(
            Long negocioId, Long turnoId, Long propietarioId) {

        Turno turno = obtenerParaGestion(
            negocioId, turnoId, propietarioId
        );

        exigirEstado(turno, EstadoTurno.LLAMADO);

        if (turno.getLlegadaEn() != null) {
            throw conflicto(
                "El cliente ya tiene su llegada registrada"
            );
        }

        LocalDateTime fecha = ahora();

        if (!plazoVencido(turno, fecha)) {
            throw conflicto(
                "Todavía no termina el plazo de tolerancia"
            );
        }

        turno.marcarAusente(fecha);

        return resumir(turno);
    }

    private Turno obtenerParaGestion(
            Long negocioId, Long turnoId, Long propietarioId) {

        bloquearDelPropietario(negocioId, propietarioId);

        return turnos.findByIdAndNegocioId(turnoId, negocioId)
            .orElseThrow(() -> noEncontrado("Turno no encontrado"));
    }

    private Negocio bloquearNegocio(Long negocioId) {
        return negocios.bloquearPorId(negocioId)
            .orElseThrow(() -> noEncontrado("Negocio no encontrado"));
    }

    private Negocio bloquearDelPropietario(
            Long negocioId, Long propietarioId) {

        bloquearNegocio(negocioId);

        return negocioService.obtenerDelPropietario(
            negocioId, propietarioId
        );
    }

    private Servicio obtenerServicio(
            Long negocioId, Long servicioId) {

        return servicios
            .findByIdAndNegocioIdAndActivoTrue(servicioId, negocioId)
            .orElseThrow(() -> noEncontrado(
                "Servicio no encontrado en este negocio"
            ));
    }

    private void comprobarRecepcion(Negocio negocio) {
        if (!negocio.isAceptaTurnos()) {
            throw conflicto(
                "El negocio no está recibiendo nuevos turnos"
            );
        }
    }

    private void exigirEstado(
            Turno turno, EstadoTurno esperado) {

        if (turno.getEstado() != esperado) {
            throw conflicto(
                "El turno debe estar en estado " + esperado
            );
        }
    }

    private boolean plazoVencido(
            Turno turno, LocalDateTime fecha) {

        LocalDateTime limite = turno.getLlamadoEn()
            .plusMinutes(turno.getToleranciaMinutos());

        return !fecha.isBefore(limite);
    }

    private TurnoResumen resumir(Turno turno) {
        Long posicion = null;
        Long espera = null;

        if (turno.getEstado() == EstadoTurno.EN_ESPERA) {
            var anteriores = turnos
                .findByNegocioIdAndEstadoInAndIdLessThanOrderByIdAsc(
                    turno.getNegocio().getId(),
                    ACTIVOS,
                    turno.getId()
                );

            posicion = (long) anteriores.size() + 1;

            LocalDateTime fecha = ahora();
            long minutos = 0;

            for (var anterior : anteriores) {
                Long restante = minutosRestantes(anterior, fecha);

                if (restante == null) {
                    return TurnoResumen.de(turno, posicion, null);
                }

                minutos += restante;
            }

            espera = minutos;

        } else if (EN_ATENCION.contains(turno.getEstado())) {
            posicion = 0L;
            espera = 0L;
        }

        return TurnoResumen.de(turno, posicion, espera);
    }

    private Long minutosRestantes(
            Turno turno, LocalDateTime fecha) {

        long duracion = turno.getServicio().getDuracionMinutos();

        if (turno.getEstado() == EstadoTurno.ATENDIENDO) {
            long segundos = Duration.between(
                turno.getInicioEn(),
                fecha
            ).getSeconds();

            long restantes = duracion * 60 - Math.max(segundos, 0);

            if (restantes <= 0) {
                return null;
            }

            return (restantes + 59) / 60;
        }

        if (turno.getEstado() == EstadoTurno.LLAMADO
                && turno.getLlegadaEn() == null) {

            LocalDateTime limite = turno.getLlamadoEn()
                .plusMinutes(turno.getToleranciaMinutos());

            long segundos = Math.max(
                Duration.between(fecha, limite).getSeconds(),
                0
            );

            return duracion + (segundos + 59) / 60;
        }

        return duracion;
    }

    private PageRequest crearPagina(
            int pagina, int tamano, Sort orden) {

        int numero = Math.max(pagina, 0);
        int cantidad = Math.min(Math.max(tamano, 1), 50);

        return PageRequest.of(numero, cantidad, orden);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(ZoneId.of("America/Lima"));
    }

    private ResponseStatusException noEncontrado(String mensaje) {
        return new ResponseStatusException(
            HttpStatus.NOT_FOUND, mensaje
        );
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(
            HttpStatus.CONFLICT, mensaje
        );
    }
}