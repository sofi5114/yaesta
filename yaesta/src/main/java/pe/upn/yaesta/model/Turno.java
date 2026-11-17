package pe.upn.yaesta.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

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

        if (origen == OrigenTurno.PRESENCIAL) {
            llegadaEn = fecha;
        }
    }

    public void registrarLlegada(LocalDateTime fecha) {
        llegadaEn = fecha;
    }

    public void llamar(LocalDateTime fecha) {
        estado = EstadoTurno.LLAMADO;
        llamadoEn = fecha;
    }

    public void iniciar(LocalDateTime fecha) {
        estado = EstadoTurno.ATENDIENDO;
        inicioEn = fecha;
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