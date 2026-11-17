package pe.upn.yaesta.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

import pe.upn.yaesta.repository.UsuarioRepository;

@Configuration
public class SecurityConfig {

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain seguridad(
                        HttpSecurity http,
                        JwtService jwtService,
                        UsuarioRepository usuarios) throws Exception {

                http
                                .csrf(csrf -> csrf.disable())

                                .sessionManagement(sesion -> sesion
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                .exceptionHandling(errores -> errores
                                                .authenticationEntryPoint((request, response, error) -> {
                                                        response.setHeader("WWW-Authenticate", "Bearer");

                                                        responder(
                                                                        response,
                                                                        HttpServletResponse.SC_UNAUTHORIZED,
                                                                        "Debes iniciar sesión");
                                                })
                                                .accessDeniedHandler((request, response, error) -> responder(
                                                                response,
                                                                HttpServletResponse.SC_FORBIDDEN,
                                                                "Tu cuenta no tiene permiso para esta operación")))

                                .authorizeHttpRequests(rutas -> rutas
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/auth/registro",
                                                                "/api/auth/login")
                                                .permitAll()
                                                .requestMatchers("/error").permitAll()
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/negocios/propios")
                                                .hasRole("NEGOCIO")

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/negocios",
                                                                "/api/negocios/*",
                                                                "/api/negocios/*/servicios",
                                                                "/api/negocios/*/servicios/*")
                                                .permitAll()

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/negocios/*/servicios")
                                                .hasRole("NEGOCIO")

                                                .requestMatchers(
                                                                HttpMethod.PUT,
                                                                "/api/negocios/*/servicios/*")
                                                .hasRole("NEGOCIO")

                                                .requestMatchers(
                                                                HttpMethod.DELETE,
                                                                "/api/negocios/*/servicios/*")
                                                .hasRole("NEGOCIO")

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/turnos",
                                                                "/api/turnos/*")
                                                .hasRole("CLIENTE")

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/negocios/*/turnos")
                                                .hasRole("CLIENTE")

                                                .requestMatchers(
                                                                HttpMethod.DELETE,
                                                                "/api/negocios/*/turnos/*")
                                                .hasRole("CLIENTE")

                                                .requestMatchers(
                                                                "/api/negocios/*/cola",
                                                                "/api/negocios/*/cola/**")
                                                .hasRole("NEGOCIO")

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/recordatorios",
                                                                "/api/recordatorios/calendario")
                                                .hasRole("CLIENTE")

                                                .requestMatchers(
                                                                HttpMethod.DELETE,
                                                                "/api/recordatorios/*")
                                                .hasRole("CLIENTE")

                                                .requestMatchers(HttpMethod.GET, "/api/perfil")
                                                .hasAnyRole("CLIENTE", "NEGOCIO")
                                                .requestMatchers(HttpMethod.PATCH, "/api/perfil/preferencias")
                                                .hasRole("CLIENTE")
                                                .anyRequest().denyAll())

                                .addFilterBefore(
                                                new JwtFiltro(jwtService, usuarios),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        private void responder(
                        HttpServletResponse response,
                        int estado,
                        String mensaje) throws java.io.IOException {

                response.setStatus(estado);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                response.getWriter().write(
                                "{\"status\":\"fail\",\"data\":{\"mensaje\":\""
                                                + mensaje + "\"}}");
        }
}
