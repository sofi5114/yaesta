package pe.upn.yaesta.service;

import static org.assertj.core.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import pe.upn.yaesta.model.*;
import pe.upn.yaesta.repository.UsuarioRepository;
import pe.upn.yaesta.security.JwtService;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "yaesta.plus.demo=false",
      "spring.datasource.url=jdbc:h2:mem:permisos;MODE=MSSQLServer;DB_CLOSE_DELAY=-1"
    })
@ActiveProfiles("test")
class PermisosHttpTest {
  @LocalServerPort int port;
  @Autowired UsuarioRepository usuarios;
  @Autowired JwtService jwt;
  @Autowired org.springframework.transaction.support.TransactionTemplate tx;
  @Autowired jakarta.persistence.EntityManager em;
  @Autowired pe.upn.yaesta.repository.NegocioRepository negocios;
  @Autowired pe.upn.yaesta.repository.ServicioRepository servicios;
  @Autowired pe.upn.yaesta.repository.MembresiaRepository membresias;
  @Autowired TurnoService cola;
  @Autowired IntercambioService intercambios;
  HttpClient cliente = HttpClient.newHttpClient();

  HttpResponse<String> pedir(String metodo, String ruta, String token, String body)
      throws Exception {
    var b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + ruta));
    if (token != null) b.header("Authorization", "Bearer " + token);
    b.header("Content-Type", "application/json");
    return cliente.send(
        b.method(metodo, HttpRequest.BodyPublishers.ofString(body)).build(),
        HttpResponse.BodyHandlers.ofString());
  }

  String token(Rol rol) {
    return jwt.generar(
        usuarios.save(new Usuario("Prueba", UUID.randomUUID() + "@test.local", "hash", rol)));
  }

  @Test
  void aceptacionConcurrenteSoloIntercambiaUnaVez() throws Exception {
    record Datos(Long intercambioId, String token, Long turnoId, Long clienteId) {}
    Datos datos =
        tx.execute(
            status -> {
              var owner =
                  usuarios.save(
                      new Usuario("Dueña", UUID.randomUUID() + "@test.local", "hash", Rol.NEGOCIO));
              var a =
                  usuarios.save(
                      new Usuario("A", UUID.randomUUID() + "@test.local", "hash", Rol.CLIENTE));
              var b =
                  usuarios.save(
                      new Usuario("B", UUID.randomUUID() + "@test.local", "hash", Rol.CLIENTE));
              em.createNativeQuery(
                      "insert into negocio"
                          + " (nombre,categoria,distrito,propietario_id,acepta_turnos,tolerancia_minutos)"
                          + " values ('Concurrencia','Belleza','Trujillo',:prop,true,5)")
                  .setParameter("prop", owner.getId())
                  .executeUpdate();
              var n = negocios.findByPropietarioIdOrderByNombreAscIdAsc(owner.getId()).getFirst();
              var servicio =
                  servicios.save(
                      new Servicio(n, "Corte", new java.math.BigDecimal("30.00"), 20, null));
              membresias.save(
                  new Membresia("CLIENTE:" + a.getId(), a.getId(), null, PlusService.ahora()));
              membresias.save(
                  new Membresia("CLIENTE:" + b.getId(), b.getId(), null, PlusService.ahora()));
              var x =
                  cola.solicitar(
                      n.getId(), a.getId(), new pe.upn.yaesta.dto.TurnoRequest(servicio.getId()));
              var y =
                  cola.solicitar(
                      n.getId(), b.getId(), new pe.upn.yaesta.dto.TurnoRequest(servicio.getId()));
              var i = intercambios.proponer(a.getId(), x.id(), y.id());
              return new Datos(i.id(), jwt.generar(b), y.id(), b.getId());
            });
    var request =
        HttpRequest.newBuilder(
                URI.create(
                    "http://127.0.0.1:" + port + "/api/plus/intercambios/" + datos.intercambioId()))
            .header("Authorization", "Bearer " + datos.token())
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"aceptar\":true}"))
            .build();
    var uno = cliente.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    var dos = cliente.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    var codigos = java.util.List.of(uno.get().statusCode(), dos.get().statusCode());
    assertThat(codigos).containsExactlyInAnyOrder(200, 409);
    assertThat(cola.miTurno(datos.clienteId(), datos.turnoId()).posicion()).isEqualTo(1);
  }

  @Test
  void membresiaNoEsPublica() throws Exception {
    assertThat(pedir("GET", "/api/plus/membresia", null, "").statusCode()).isEqualTo(401);
  }

  @Test
  void produccionNoPermiteActivarPlusGratis() throws Exception {
    var r = pedir("POST", "/api/plus/prueba", token(Rol.CLIENTE), "{}");
    assertThat(r.statusCode()).isEqualTo(409);
    assertThat(r.body()).contains("deshabilitadas");
  }

  @Test
  void negocioNoPuedeIntercambiarTurnosDeClientes() throws Exception {
    assertThat(pedir("GET", "/api/plus/intercambios", token(Rol.NEGOCIO), "").statusCode())
        .isEqualTo(403);
  }

  @Test
  void solicitudIncompletaNoSeProcesa() throws Exception {
    assertThat(pedir("POST", "/api/plus/intercambios", token(Rol.CLIENTE), "{}").statusCode())
        .isEqualTo(400);
  }

  @Test
  void coordenadasFueraDeRangoSeRechazan() throws Exception {
    assertThat(
            pedir(
                    "GET",
                    "/api/turnos/1/alternativas?latitud=999&longitud=0",
                    token(Rol.CLIENTE),
                    "")
                .statusCode())
        .isEqualTo(400);
  }
}
