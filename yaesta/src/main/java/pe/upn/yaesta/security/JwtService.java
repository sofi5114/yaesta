package pe.upn.yaesta.security;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import pe.upn.yaesta.model.Usuario;

@Service
public class JwtService {

    private final SecretKey clave;
    private final long minutos;

    public JwtService(
            @Value("${yaesta.jwt.secreto}") String secreto,
            @Value("${yaesta.jwt.minutos}") long minutos) {

        this.clave = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(secreto)
        );
        this.minutos = minutos;
    }

    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();

        return Jwts.builder()
            .subject(usuario.getId().toString())
            .claim("rol", usuario.getRol().name())
            .issuedAt(Date.from(ahora))
            .expiration(Date.from(ahora.plusSeconds(minutos * 60)))
            .signWith(clave)
            .compact();
    }

    public Claims verificar(String token) {
        return Jwts.parser()
            .verifyWith(clave)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}