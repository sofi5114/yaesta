package pe.upn.yaesta.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

import pe.upn.yaesta.dto.Jsend;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Jsend<Map<String, String>>> validacion(
            MethodArgumentNotValidException error) {

        Map<String, String> campos = new LinkedHashMap<>();

        error.getBindingResult().getFieldErrors().forEach(
            campo -> campos.putIfAbsent(
                campo.getField(),
                campo.getDefaultMessage()
            )
        );

        return ResponseEntity.badRequest()
            .body(Jsend.fail(campos));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Jsend<Map<String, String>>> recurso(
            ResponseStatusException error) {

        String mensaje = error.getReason();

        if (mensaje == null) {
            mensaje = "No se pudo completar la solicitud";
        }

        return ResponseEntity.status(error.getStatusCode())
            .body(Jsend.fail(Map.of("mensaje", mensaje)));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<Jsend<Map<String, String>>> integridad(
        DataIntegrityViolationException error) {

    return ResponseEntity.status(409)
        .body(Jsend.fail(
            Map.of(
                "mensaje",
                "Los datos entran en conflicto con un registro existente"
            )
        ));
}
}