package pe.upn.yaesta.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import pe.upn.yaesta.model.Usuario;
import pe.upn.yaesta.repository.UsuarioRepository;

public class JwtFiltro extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarios;

    public JwtFiltro(JwtService jwtService,
                     UsuarioRepository usuarios) {
        this.jwtService = jwtService;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain cadena)
            throws ServletException, IOException {

        String cabecera = request.getHeader("Authorization");

        if (cabecera == null || !cabecera.startsWith("Bearer ")) {
            cadena.doFilter(request, response);
            return;
        }

        String token = cabecera.substring(7);
        Long usuarioId;

        try {
            usuarioId = Long.valueOf(
                jwtService.verificar(token).getSubject()
            );
        } catch (JwtException | IllegalArgumentException error) {
            rechazar(response);
            return;
        }

        Usuario usuario = usuarios.findById(usuarioId).orElse(null);

        if (usuario == null || !usuario.isActivo()) {
            rechazar(response);
            return;
        }

        var permisos = List.of(
            new SimpleGrantedAuthority(
                "ROLE_" + usuario.getRol().name()
            )
        );

        var autenticacion =
            new UsernamePasswordAuthenticationToken(
                usuario.getId(),
                null,
                permisos
            );

        SecurityContextHolder.getContext()
            .setAuthentication(autenticacion);

        cadena.doFilter(request, response);
    }

    private void rechazar(HttpServletResponse response)
            throws IOException {

        SecurityContextHolder.clearContext();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Bearer");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
            """
            {
              "status": "fail",
              "data": {
                "mensaje": "La sesión no es válida. Inicia sesión nuevamente."
              }
            }
            """
        );
    }
}