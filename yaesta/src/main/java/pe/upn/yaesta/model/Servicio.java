package pe.upn.yaesta.model;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "servicio")
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Column(nullable = false, length = 100,
            columnDefinition = "nvarchar(100)")
    private String nombre;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "duracion_minutos", nullable = false)
    private int duracionMinutos;

    @Column(name = "dias_recordatorio")
    private Integer diasRecordatorio;

    @Column(nullable = false)
    private boolean activo = true;

    protected Servicio() {
    }

    public Servicio(Negocio negocio, String nombre, BigDecimal precio,
                    int duracionMinutos, Integer diasRecordatorio) {
        this.negocio = negocio;
        this.nombre = nombre;
        this.precio = precio;
        this.duracionMinutos = duracionMinutos;
        this.diasRecordatorio = diasRecordatorio;
    }

    public void actualizar(String nombre, BigDecimal precio,
                           int duracionMinutos, Integer diasRecordatorio) {
        this.nombre = nombre;
        this.precio = precio;
        this.duracionMinutos = duracionMinutos;
        this.diasRecordatorio = diasRecordatorio;
    }

    public void desactivar() {
        activo = false;
    }

    public Long getId() {
        return id;
    }

    public Negocio getNegocio() {
        return negocio;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public Integer getDiasRecordatorio() {
        return diasRecordatorio;
    }

    public boolean isActivo() {
        return activo;
    }
}