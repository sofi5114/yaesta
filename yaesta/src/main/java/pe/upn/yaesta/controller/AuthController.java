package pe.upn.yaesta.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import pe.upn.yaesta.dto.Jsend;
import pe.upn.yaesta.dto.LoginRequest;
import pe.upn.yaesta.dto.LoginResponse;
import pe.upn.yaesta.dto.RegistroRequest;
import pe.upn.yaesta.dto.UsuarioResumen;
import pe.upn.yaesta.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService servicio;

    public AuthController(AuthService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/registro")
    public ResponseEntity<Jsend<UsuarioResumen>> registrar(
            @Valid @RequestBody RegistroRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Jsend.success(servicio.registrar(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<Jsend<LoginResponse>> ingresar(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
            Jsend.success(servicio.ingresar(request))
        );
    }
}