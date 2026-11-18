package pe.upn.yaesta.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "seguimiento",
    uniqueConstraints = @UniqueConstraint(columnNames = {"cliente_id", "negocio_id"}))
public class Seguimiento {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cliente_id", nullable = false)
  private Long clienteId;

  @Column(name = "negocio_id", nullable = false)
  private Long negocioId;

  @Column(nullable = false)
  private boolean alertas;

  public Seguimiento(Long clienteId, Long negocioId, boolean alertas) {
    this.clienteId = clienteId;
    this.negocioId = negocioId;
    this.alertas = alertas;
  }

  public void cambiarAlertas(boolean valor) {
    alertas = valor;
  }
}
