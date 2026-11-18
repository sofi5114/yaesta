package pe.upn.yaesta.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "evento_turno")
public class EventoTurno {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long turnoId;

  private Long actorId;

  @Column(nullable = false, length = 40)
  private String accion;

  @Column(nullable = false, length = 240, columnDefinition = "nvarchar(240)")
  private String detalle;

  @Column(nullable = false)
  private LocalDateTime fecha;

  public EventoTurno(
      Long turnoId, Long actorId, String accion, String detalle, LocalDateTime fecha) {
    this.turnoId = turnoId;
    this.actorId = actorId;
    this.accion = accion;
    this.detalle = detalle;
    this.fecha = fecha;
  }
}
