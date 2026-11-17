package pe.upn.yaesta.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "recordatorio")
public class Recordatorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_id", nullable = false, unique = true)
    private Turno turno;

    @Column(nullable = false)
    private LocalDate fechaProgramada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoRecordatorio estado;

    private LocalDateTime enviadoEn;

    public Recordatorio(Turno turno, LocalDate fechaProgramada) {
        this.turno = turno;
        this.fechaProgramada = fechaProgramada;
        this.estado = EstadoRecordatorio.PENDIENTE;
    }

    public void iniciarEnvio() {
        estado = EstadoRecordatorio.PROCESANDO;
    }

    public void marcarEnviado(LocalDateTime fecha) {
        estado = EstadoRecordatorio.ENVIADO;
        enviadoEn = fecha;
    }

    public void marcarError() {
        estado = EstadoRecordatorio.ERROR;
    }

    public void cancelar() {
        estado = EstadoRecordatorio.CANCELADO;
    }
}