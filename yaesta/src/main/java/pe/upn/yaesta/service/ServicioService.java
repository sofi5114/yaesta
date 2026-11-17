package pe.upn.yaesta.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.upn.yaesta.dto.Pagina;
import pe.upn.yaesta.dto.ServicioRequest;
import pe.upn.yaesta.dto.ServicioResumen;
import pe.upn.yaesta.model.Negocio;
import pe.upn.yaesta.model.Servicio;
import pe.upn.yaesta.repository.ServicioRepository;

@Service
@Transactional(readOnly = true)
public class ServicioService {

    private final ServicioRepository servicios;
    private final NegocioService negocioService;

    public ServicioService(ServicioRepository servicios,
                           NegocioService negocioService) {
        this.servicios = servicios;
        this.negocioService = negocioService;
    }

    public Pagina<ServicioResumen> listar(
            Long negocioId, int pagina, int tamano) {

        negocioService.buscarPorId(negocioId);

        int numero = Math.max(pagina, 0);
        int cantidad = Math.min(Math.max(tamano, 1), 50);

        var pedido = PageRequest.of(
            numero,
            cantidad,
            Sort.by("nombre", "id")
        );

        var resultado = servicios
            .findByNegocioIdAndActivoTrue(negocioId, pedido)
            .map(ServicioResumen::de);

        return Pagina.de(resultado);
    }

    public ServicioResumen buscar(Long negocioId, Long servicioId) {
        negocioService.buscarPorId(negocioId);
        return ServicioResumen.de(obtener(negocioId, servicioId));
    }

    @Transactional
public ServicioResumen crear(
        Long negocioId, Long usuarioId, ServicioRequest request) {

    Negocio negocio = negocioService.obtenerDelPropietario(
        negocioId, usuarioId
    );

    Servicio servicio = new Servicio(
        negocio,
        request.nombre().trim(),
        request.precio(),
        request.duracionMinutos(),
        request.diasRecordatorio()
    );

    return ServicioResumen.de(servicios.save(servicio));
}

    @Transactional
public ServicioResumen actualizar(
        Long negocioId, Long servicioId,
        Long usuarioId, ServicioRequest request) {

    negocioService.obtenerDelPropietario(negocioId, usuarioId);

    Servicio servicio = obtener(negocioId, servicioId);

    servicio.actualizar(
        request.nombre().trim(),
        request.precio(),
        request.duracionMinutos(),
        request.diasRecordatorio()
    );

    return ServicioResumen.de(servicio);
}

    @Transactional
public void desactivar(
        Long negocioId, Long servicioId, Long usuarioId) {

    negocioService.obtenerDelPropietario(negocioId, usuarioId);

    obtener(negocioId, servicioId).desactivar();
}

private Servicio obtener(Long negocioId, Long servicioId) {
    return servicios
        .findByIdAndNegocioIdAndActivoTrue(servicioId, negocioId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Servicio no encontrado en este negocio"
        ));
}
}