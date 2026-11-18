package pe.upn.yaesta.model;

import jakarta.persistence.*;

@Entity
@Table(name = "negocio")
public class Negocio {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "propietario_id")
  private Usuario propietario;

  @Column(nullable = false, length = 100)
  private String nombre;

  @Column(nullable = false, length = 60)
  private String categoria;

  @Column(nullable = false, length = 60)
  private String distrito;

  @Column(name = "acepta_turnos", nullable = false)
  private boolean aceptaTurnos = false;

  @Column(name = "tolerancia_minutos", nullable = false)
  private int toleranciaMinutos = 5;

  private Double latitud;
  private Double longitud;
  private java.time.LocalDateTime disponibilidadActualizada;
  private Integer descuentoDerivacion;
  private Integer cuposDerivacion;

  public Double getLatitud() {
    return latitud;
  }

  public Double getLongitud() {
    return longitud;
  }

  public java.time.LocalDateTime getDisponibilidadActualizada() {
    return disponibilidadActualizada;
  }

  public int getDescuentoDerivacion() {
    return descuentoDerivacion == null ? 0 : descuentoDerivacion;
  }

  public int getCuposDerivacion() {
    return cuposDerivacion == null ? 0 : cuposDerivacion;
  }

  public void actualizarDisponibilidad(java.time.LocalDateTime fecha) {
    disponibilidadActualizada = fecha;
  }

  public void ubicar(double latitud, double longitud, int descuento, int cupos) {
    this.latitud = latitud;
    this.longitud = longitud;
    descuentoDerivacion = descuento;
    cuposDerivacion = cupos;
  }

  public void consumirCupoDerivacion() {
    cuposDerivacion = getCuposDerivacion() - 1;
  }

  protected Negocio() {}

  public Long getId() {
    return id;
  }

  public Usuario getPropietario() {
    return propietario;
  }

  public String getNombre() {
    return nombre;
  }

  public String getCategoria() {
    return categoria;
  }

  public String getDistrito() {
    return distrito;
  }

  public boolean isAceptaTurnos() {
    return aceptaTurnos;
  }

  public int getToleranciaMinutos() {
    return toleranciaMinutos;
  }

  public void cambiarRecepcionTurnos(boolean acepta) {
    aceptaTurnos = acepta;
  }

  public void cambiarTolerancia(int minutos) {
    toleranciaMinutos = minutos;
  }
}
