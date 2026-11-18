package pe.upn.yaesta.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "intercambio")
public class Intercambio {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long negocioId;

  @Column(nullable = false)
  private Long origenId;

  @Column(nullable = false)
  private Long destinoId;

  @Column(nullable = false)
  private Long solicitanteId;

  @Column(nullable = false)
  private Long receptorId;

  @Column(nullable = false)
  private long ordenOrigen;

  @Column(nullable = false)
  private long ordenDestino;

  @Column(nullable = false)
  private LocalDateTime venceEn;

  @Column(nullable = false, length = 15)
  private String estado = "PENDIENTE";

  public Intercambio(Turno origen, Turno destino, LocalDateTime fecha) {
    negocioId = origen.getNegocio().getId();
    origenId = origen.getId();
    destinoId = destino.getId();
    solicitanteId = origen.getCliente().getId();
    receptorId = destino.getCliente().getId();
    ordenOrigen = origen.orden();
    ordenDestino = destino.orden();
    venceEn = fecha.plusMinutes(3);
  }

  public void resolver(String estado) {
    this.estado = estado;
  }
}
