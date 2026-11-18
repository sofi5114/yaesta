package pe.upn.yaesta.service;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.upn.yaesta.dto.*;
import pe.upn.yaesta.model.*;
import pe.upn.yaesta.repository.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FlujosTest {
  @Autowired EntityManager em;
  @Autowired TurnoService cola;
  @Autowired PlusService plus;
  @Autowired IntercambioService cambios;
  @Autowired AlternativasService alternativas;
  @Autowired UsuarioRepository usuarios;
  @Autowired NegocioRepository negocios;
  @Autowired ServicioRepository servicios;
  @Autowired TurnoRepository turnos;
  @Autowired MembresiaRepository membresias;
  Usuario propietario, a, b, c;
  Negocio negocio, otro;
  Servicio servicio, otroServicio;

  @BeforeEach
  void datos() {
    propietario =
        usuarios.save(new Usuario("Dueña", UUID.randomUUID() + "@test.local", "hash", Rol.NEGOCIO));
    a = usuarios.save(new Usuario("Ana", UUID.randomUUID() + "@test.local", "hash", Rol.CLIENTE));
    b = usuarios.save(new Usuario("Beto", UUID.randomUUID() + "@test.local", "hash", Rol.CLIENTE));
    c = usuarios.save(new Usuario("Caro", UUID.randomUUID() + "@test.local", "hash", Rol.CLIENTE));
    negocio = crearNegocio("Primero");
    otro = crearNegocio("Alternativa");
    servicio = servicios.save(new Servicio(negocio, "Corte", new BigDecimal("30.00"), 20, null));
    otroServicio = servicios.save(new Servicio(otro, "Corte", new BigDecimal("30.00"), 20, null));
    em.flush();
  }

  Negocio crearNegocio(String nombre) {
    var n = em.getCriteriaBuilder();
    em.createNativeQuery(
            "insert into negocio"
                + " (nombre,categoria,distrito,propietario_id,acepta_turnos,tolerancia_minutos)"
                + " values (:nombre,'Belleza','Trujillo',:prop,true,5)")
        .setParameter("nombre", nombre)
        .setParameter("prop", propietario.getId())
        .executeUpdate();
    return negocios.findByPropietarioIdOrderByNombreAscIdAsc(propietario.getId()).stream()
        .filter(v -> v.getNombre().equals(nombre))
        .findFirst()
        .orElseThrow();
  }

  Turno turno(Usuario u) {
    return turnos
        .findById(
            cola.solicitar(negocio.getId(), u.getId(), new TurnoRequest(servicio.getId())).id())
        .orElseThrow();
  }

  @Test
  void ausenciaAutomaticaSoloSinLlegadaYAlVencer() {
    var t = turno(a);
    var fecha = PlusService.ahora();
    t.llamar(fecha);
    cola.vencerAusencia(negocio.getId(), t.getId(), fecha.plusMinutes(4));
    assertThat(t.getEstado()).isEqualTo(EstadoTurno.LLAMADO);
    cola.vencerAusencia(negocio.getId(), t.getId(), fecha.plusMinutes(5));
    assertThat(t.getEstado()).isEqualTo(EstadoTurno.AUSENTE);
    cola.vencerAusencia(negocio.getId(), t.getId(), fecha.plusMinutes(6));
    assertThat(t.getEstado()).isEqualTo(EstadoTurno.AUSENTE);
  }

  @Test
  void llegadaConfirmadaNoVencePorDemoraDelNegocio() {
    var t = turno(a);
    var fecha = PlusService.ahora();
    t.llamar(fecha);
    t.registrarLlegada(fecha.plusMinutes(1));
    cola.vencerAusencia(negocio.getId(), t.getId(), fecha.plusHours(1));
    assertThat(t.getEstado()).isEqualTo(EstadoTurno.LLAMADO);
  }

  @Test
  void intercambioRequiereAceptacionYConservaTerceros() {
    plus.activarPrueba(a.getId(), null);
    plus.activarPrueba(c.getId(), null);
    var primero = turno(a);
    var medio = turno(b);
    var ultimo = turno(c);
    var i = cambios.proponer(a.getId(), primero.getId(), ultimo.getId());
    assertThat(primero.orden()).isEqualTo(primero.getId());
    assertThatThrownBy(() -> cambios.resolver(a.getId(), i.id(), true))
        .isInstanceOf(ResponseStatusException.class);
    cambios.resolver(c.getId(), i.id(), true);
    assertThat(primero.getEsperaInicialMinutos()).isNull();
    assertThat(ultimo.getEsperaInicialMinutos()).isNull();
    assertThat(cola.miTurno(b.getId(), medio.getId()).posicion()).isEqualTo(2);
    assertThat(cola.llamarSiguiente(negocio.getId(), propietario.getId()).id())
        .isEqualTo(ultimo.getId());
    assertThatThrownBy(() -> cambios.resolver(c.getId(), i.id(), true))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void sinPlusOServicioDistintoNoHayIntercambio() {
    var x = turno(a);
    var y = turno(b);
    assertThatThrownBy(() -> cambios.proponer(a.getId(), x.getId(), y.getId()))
        .isInstanceOf(ResponseStatusException.class);
    plus.activarPrueba(a.getId(), null);
    plus.activarPrueba(b.getId(), null);
    y.registrarLlegada(PlusService.ahora());
    assertThatThrownBy(() -> cambios.proponer(a.getId(), x.getId(), y.getId()))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void duracionSeConservaAunqueCambieElCatalogo() {
    var t = turno(a);
    servicio.actualizar("Corte", new BigDecimal("35.00"), 90, null);
    assertThat(t.duracion()).isEqualTo(20);
    assertThat(t.getPrecioServicio()).isEqualByComparingTo("30.00");
  }

  @Test
  void canalGratisSoloConNegocioPlusYPermisosDelDueno() {
    assertThatThrownBy(() -> plus.seguir(a.getId(), negocio.getId(), false))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> plus.activarPrueba(a.getId(), negocio.getId()))
        .isInstanceOf(ResponseStatusException.class);
    plus.activarPrueba(propietario.getId(), negocio.getId());
    plus.seguir(a.getId(), negocio.getId(), false);
    plus.seguir(a.getId(), negocio.getId(), true);
    assertThat(plus.estado(a.getId(), negocio.getId()).seguidores()).isEqualTo(1);
    plus.publicar(
        propietario.getId(),
        negocio.getId(),
        new PublicacionRequest("Disponible", "Tenemos un espacio", 2));
    assertThat(plus.novedades(a.getId(), 0).contenido()).hasSize(1);
    plus.dejarSeguir(a.getId(), negocio.getId());
    assertThat(plus.novedades(a.getId(), 0).contenido()).isEmpty();
  }

  @Test
  void pruebaNoSePuedeRepetirYVigenciaSeCompruebaEnServidor() {
    plus.activarPrueba(a.getId(), null);
    plus.cancelar(a.getId(), null);
    assertThat(plus.estado(a.getId(), null).activa()).isFalse();
    assertThatThrownBy(() -> plus.activarPrueba(a.getId(), null))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> plus.exigirClientePlus(a.getId()))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void demoraActualizaLaEsperaSinInventarCero() {
    var t = turno(a);
    var siguiente = turno(b);
    var fecha = PlusService.ahora().minusMinutes(30);
    t.llamar(fecha);
    t.registrarLlegada(fecha);
    t.iniciar(fecha);
    assertThat(cola.miTurno(b.getId(), siguiente.getId()).esperaEstimadaMinutos()).isNull();
    cola.actualizarDemora(negocio.getId(), t.getId(), propietario.getId(), 12);
    assertThat(cola.miTurno(b.getId(), siguiente.getId()).esperaEstimadaMinutos())
        .isBetween(11L, 12L);
  }

  @Test
  void derivacionValidaPrecioYEsAtomicaConBeneficio() {
    plus.activarPrueba(propietario.getId(), otro.getId());
    alternativas.configurar(propietario.getId(), otro.getId(), -8.11, -79.03, 10, 1);
    var origen = turno(a);
    origen.registrarEstimacionInicial(0L);
    // Force a verifiable increase of more than ten minutes in the original queue.
    var delante = turno(b);
    long orden = origen.orden();
    origen.intercambiarOrden(delante.orden());
    delante.intercambiarOrden(orden);
    var opciones = alternativas.buscar(a.getId(), origen.getId(), -8.11, -79.03);
    assertThat(opciones).hasSize(1);
    assertThat(opciones.getFirst().descuento()).isEqualTo(10);
    assertThatThrownBy(
            () ->
                alternativas.cambiar(
                    a.getId(), origen.getId(), otroServicio.getId(), new BigDecimal("1.00"), 7))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(origen.getEstado()).isEqualTo(EstadoTurno.EN_ESPERA);
    var nuevo =
        alternativas.cambiar(
            a.getId(), origen.getId(), otroServicio.getId(), new BigDecimal("27.00"), 7);
    assertThat(nuevo.precioServicio()).isEqualByComparingTo("27.00");
    assertThat(origen.getEstado()).isEqualTo(EstadoTurno.CANCELADO);
    assertThat(otro.getCuposDerivacion()).isZero();
  }

  @Test
  void ubicacionAntiguaNoSeRecomiendaYNuncaSeCancelaElTurnoAlBuscar() {
    otro.ubicar(-8.11, -79.03, 0, 0);
    otro.actualizarDisponibilidad(PlusService.ahora().minusMinutes(11));
    var t = turno(a);
    assertThat(alternativas.buscar(a.getId(), t.getId(), -8.11, -79.03)).isEmpty();
    assertThat(t.getEstado()).isEqualTo(EstadoTurno.EN_ESPERA);
  }

  @Test
  void distanciaNoEsTiempoDeViaje() {
    assertThat(AlternativasService.distancia(-8.11, -79.03, -8.11, -79.03)).isZero();
    assertThat(AlternativasService.distancia(0, 0, 0, 1)).isBetween(111.0, 112.0);
  }
  @Test void trasladoProtegidoPermiteAtenderAOtroYNoVence() {
    var viajero=turno(a);viajero.protegerTraslado(PlusService.ahora().plusMinutes(15));
    var presente=turno(b);
    assertThat(cola.miTurno(b.getId(),presente.getId()).posicion()).isEqualTo(1);
    assertThat(cola.llamarSiguiente(negocio.getId(),propietario.getId()).id()).isEqualTo(presente.getId());
    cola.vencerAusencia(negocio.getId(),viajero.getId(),PlusService.ahora().plusHours(1));
    assertThat(viajero.getEstado()).isEqualTo(EstadoTurno.EN_ESPERA);
  }
  @Test void llegadaAdelantadaLiberaLaProteccionDeTraslado() {
    var t=turno(a);t.protegerTraslado(PlusService.ahora().plusMinutes(15));
    assertThatThrownBy(()->cola.llamarSiguiente(negocio.getId(),propietario.getId())).isInstanceOf(ResponseStatusException.class);
    cola.registrarLlegada(negocio.getId(),t.getId(),propietario.getId());
    assertThat(cola.llamarSiguiente(negocio.getId(),propietario.getId()).id()).isEqualTo(t.getId());
  }

}
