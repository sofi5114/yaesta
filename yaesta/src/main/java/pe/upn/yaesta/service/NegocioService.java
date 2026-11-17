package pe.upn.yaesta.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.upn.yaesta.dto.NegocioResumen;
import pe.upn.yaesta.model.Negocio;
import pe.upn.yaesta.repository.NegocioRepository;

@Service
@Transactional(readOnly = true)
public class NegocioService {

    private final NegocioRepository repositorio;

    public NegocioService(NegocioRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<NegocioResumen> listar() {
        return repositorio.findAll().stream()
            .map(NegocioResumen::de)
            .toList();
    }

    public NegocioResumen buscar(Long id) {
        return NegocioResumen.de(buscarPorId(id));
    }

    public Negocio buscarPorId(Long id) {
        return repositorio.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Negocio no encontrado"
            ));
    }

    public Negocio obtenerDelPropietario(
        Long negocioId, Long usuarioId) {

    Negocio negocio = buscarPorId(negocioId);

    if (negocio.getPropietario() == null
            || !negocio.getPropietario().getId().equals(usuarioId)) {

        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "No tienes permiso para modificar este negocio"
        );
    }
    
    return negocio;

    
}

public List<NegocioResumen> misNegocios(Long propietarioId) {
    return repositorio
        .findByPropietarioIdOrderByNombreAscIdAsc(propietarioId)
        .stream()
        .map(NegocioResumen::de)
        .toList();
}
}