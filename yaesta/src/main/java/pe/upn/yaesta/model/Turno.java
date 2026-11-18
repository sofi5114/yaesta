package pe.upn.yaesta.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "turno")
public class Turno {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "negocio_id", nullable = false)
  private Negocio negocio;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "servicio_id", nullable = false)
  private Servicio servicio;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cliente_id")
  private Usuario cliente;

  @Column(length = 100, columnDefinition = "nvarchar(100)")
  private String nombrePresencial;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 15)
  private EstadoTurno estado;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrigenTurno origen;

  @Column(nullable = false)
  private LocalDateTime solicitadoEn;

  private LocalDateTime llegadaEn;
  private LocalDateTime llamadoEn;
  private LocalDateTime inicioEn;
  private LocalDateTime finEn;

  @Column(nullable = false)
  private int toleranciaMinutos;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal precioServicio;

  private LocalDateTime noLlamarAntesDe;
  public void protegerTraslado(LocalDateTime fecha) { noLlamarAntesDe = fecha; }
  public boolean disponibleParaLlamado(LocalDateTime fecha) { return noLlamarAntesDe == null || !fecha.isBefore(noLlamarAntesDe); }
  private Long esperaInicialMinutos;

  public void registrarEstimacionInicial(Long minutos) {
    esperaInicialMinutos = minutos;
  }

  public boolean demoraSignificativa(Long esperaActual, LocalDateTime fecha) {
    if (esperaInicialMinutos == null) return false;
    var limite = solicitadoEn.plusMinutes(esperaInicialMinutos + 10);
    return !fecha.plusMinutes(esperaActual == null ? 0 : esperaActual).isBefore(limite);
  }

  private Long derivadoDesde;
  private Integer descuentoAplicado;

  public void registrarDerivacion(Long origen, int porcentaje) {
    derivadoDesde = origen;
    descuentoAplicado = porcentaje;
    precioServicio =
        precioServicio
            .multiply(java.math.BigDecimal.valueOf(100 - porcentaje))
            .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
  }

  private LocalDateTime finEstimado;
  private Long ordenCola;
  private Integer intercambios;
  private Integer duracionReservada;

  public long orden() {
    return ordenCola == null ? id : ordenCola;
  }

  public int duracion() {
    return duracionReservada == null ? servicio.getDuracionMinutos() : duracionReservada;
  }

  public int cambios() {
    return intercambios == null ? 0 : intercambios;
  }

  public void intercambiarOrden(long nuevo) {
    ordenCola = nuevo;
    intercambios = cambios() + 1;
  }

  public void actualizarFinEstimado(LocalDateTime fecha) {
    finEstimado = fecha;
  }

  public Turno(
      Servicio servicio,
      Usuario cliente,
      String nombrePresencial,
      OrigenTurno origen,
      LocalDateTime fecha) {

    this.servicio = servicio;
    negocio = servicio.getNegocio();
    this.cliente = cliente;
    this.nombrePresencial = nombrePresencial;
    this.origen = origen;

    estado = EstadoTurno.EN_ESPERA;
    solicitadoEn = fecha;
    toleranciaMinutos = negocio.getToleranciaMinutos();
    precioServicio = servicio.getPrecio();
    duracionReservada = servicio.getDuracionMinutos();

    if (origen == OrigenTurno.PRESENCIAL) {
      llegadaEn = fecha;
    noLlamarAntesDe = null;
    }
  }

  public void registrarLlegada(LocalDateTime fecha) {
    llegadaEn = fecha;
    noLlamarAntesDe = null;
  }

  public void llamar(LocalDateTime fecha) {
    estado = EstadoTurno.LLAMADO;
    llamadoEn = fecha;
  }

  public void iniciar(LocalDateTime fecha) {
    estado = EstadoTurno.ATENDIENDO;
    inicioEn = fecha;
    finEstimado = fecha.plusMinutes(duracion());
  }

  public void completar(LocalDateTime fecha) {
    estado = EstadoTurno.ATENDIDO;
    finEn = fecha;
  }

  public void cancelar(LocalDateTime fecha) {
    estado = EstadoTurno.CANCELADO;
    finEn = fecha;
  }

  public void marcarAusente(LocalDateTime fecha) {
    estado = EstadoTurno.AUSENTE;
    finEn = fecha;
  }
}
