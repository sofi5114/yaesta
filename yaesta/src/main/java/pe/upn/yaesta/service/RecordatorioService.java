package pe.upn.yaesta.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

import pe.upn.yaesta.dto.Pagina;
import pe.upn.yaesta.dto.RecordatorioResumen;
import pe.upn.yaesta.model.*;
import pe.upn.yaesta.repository.RecordatorioRepository;

import java.time.YearMonth;
import java.time.DateTimeException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordatorioService {

    private static final List<EstadoRecordatorio> CANCELABLES = List.of(
            EstadoRecordatorio.PENDIENTE,
            EstadoRecordatorio.ERROR);

    private final RecordatorioRepository recordatorios;

    public Pagina<RecordatorioResumen> misRecordatorios(
            Long clienteId, int pagina, int tamano) {

        int numero = Math.max(pagina, 0);
        int cantidad = Math.min(Math.max(tamano, 1), 50);

        var pedido = PageRequest.of(
                numero,
                cantidad,
                Sort.by("fechaProgramada", "id"));

        return Pagina.de(
                recordatorios.findByTurnoClienteId(clienteId, pedido)
                        .map(RecordatorioResumen::de));
    }

    @Transactional
    public void generar(Turno turno) {
        if (turno.getEstado() != EstadoTurno.ATENDIDO
                || turno.getFinEn() == null) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El recordatorio requiere una atención completada");
        }

        Usuario cliente = turno.getCliente();

        if (cliente == null) {
            return;
        }

        var anteriores = recordatorios
                .findByTurnoClienteIdAndTurnoServicioIdAndEstadoIn(
                        cliente.getId(),
                        turno.getServicio().getId(),
                        CANCELABLES);

        for (var recordatorio : anteriores) {
            recordatorio.cancelar();
        }

        Integer dias = turno.getServicio().getDiasRecordatorio();

        if (!cliente.isAceptaRecordatorios() || dias == null) {
            return;
        }

        var fecha = turno.getFinEn()
                .toLocalDate()
                .plusDays(dias);

        recordatorios.save(new Recordatorio(turno, fecha));
    }

    @Transactional
    public RecordatorioResumen cancelar(
            Long clienteId, Long recordatorioId) {

        Recordatorio recordatorio = recordatorios
                .findByIdAndTurnoClienteId(recordatorioId, clienteId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Recordatorio no encontrado"));

        if (!CANCELABLES.contains(recordatorio.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El recordatorio ya no se puede cancelar");
        }

        recordatorio.cancelar();

        return RecordatorioResumen.de(recordatorio);
    }

    public List<RecordatorioResumen> calendario(
        Long clienteId, int anio, int mes) {

    YearMonth periodo;

    try {
        periodo = YearMonth.of(anio, mes);
    } catch (DateTimeException error) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "El año o el mes no son válidos"
        );
    }

    return recordatorios
        .findByTurnoClienteIdAndFechaProgramadaBetweenOrderByFechaProgramadaAscIdAsc(
            clienteId,
            periodo.atDay(1),
            periodo.atEndOfMonth()
        )
        .stream()
        .map(RecordatorioResumen::de)
        .toList();
}
}
