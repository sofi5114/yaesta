package pe.upn.yaesta.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.upn.yaesta.dto.LoginRequest;
import pe.upn.yaesta.dto.LoginResponse;
import pe.upn.yaesta.dto.RegistroRequest;
import pe.upn.yaesta.dto.UsuarioResumen;
import pe.upn.yaesta.model.Rol;
import pe.upn.yaesta.model.Usuario;
import pe.upn.yaesta.repository.UsuarioRepository;
import pe.upn.yaesta.security.JwtService;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository usuarios,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UsuarioResumen registrar(RegistroRequest request) {
        validarLongitudClave(request.clave());

        String correo = normalizarCorreo(request.correo());

        if (usuarios.existsByCorreo(correo)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El correo ya está registrado"
            );
        }

        Usuario usuario = new Usuario(
            request.nombre().trim(),
            correo,
            passwordEncoder.encode(request.clave()),
            Rol.CLIENTE
        );

        usuario.cambiarRecordatorios(
            request.aceptaRecordatorios()
        );

        return UsuarioResumen.de(usuarios.save(usuario));
    }

    @Transactional(readOnly = true)
    public LoginResponse ingresar(LoginRequest request) {
        validarLongitudClave(request.clave());

        String correo = normalizarCorreo(request.correo());

        Usuario usuario = usuarios.findByCorreo(correo)
            .orElseThrow(() -> credencialesInvalidas());

        boolean claveCorrecta = passwordEncoder.matches(
            request.clave(),
            usuario.getClaveHash()
        );

        if (!usuario.isActivo() || !claveCorrecta) {
            throw credencialesInvalidas();
        }

        return new LoginResponse(
            jwtService.generar(usuario),
            UsuarioResumen.de(usuario)
        );
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private void validarLongitudClave(String clave) {
        int cantidadBytes = clave
            .getBytes(StandardCharsets.UTF_8)
            .length;

        if (cantidadBytes > 72) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La contraseña admite hasta 72 bytes"
            );
        }
    }

    private ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Correo o contraseña incorrectos"
        );
    }
}
