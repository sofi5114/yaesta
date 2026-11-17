package pe.upn.yaesta.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record Pagina<T>(
    List<T> contenido,
    int pagina,
    int tamano,
    long totalElementos,
    int totalPaginas
) {

    public static <T> Pagina<T> de(Page<T> resultado) {
        return new Pagina<>(
            resultado.getContent(),
            resultado.getNumber(),
            resultado.getSize(),
            resultado.getTotalElements(),
            resultado.getTotalPages()
        );
    }
}