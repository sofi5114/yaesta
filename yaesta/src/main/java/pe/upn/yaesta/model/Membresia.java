package pe.upn.yaesta.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "membresia", uniqueConstraints = @UniqueConstraint(columnNames = "clave"))
public class Membresia {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 80)
  private String clave;

  @Column(nullable = false)
  private Long usuarioId;

  private Long negocioId;

  @Column(nullable = false)
  private LocalDateTime inicio;

  @Column(nullable = false)
  private LocalDateTime fin;

  @Column(nullable = false)
  private boolean demostracion;

  public Membresia(String clave, Long usuarioId, Long negocioId, LocalDateTime fecha) {
    this.clave = clave;
    this.usuarioId = usuarioId;
    this.negocioId = negocioId;
    inicio = fecha;
    fin = fecha.plusDays(30);
    demostracion = true;
  }

  public boolean vigente(LocalDateTime fecha) {
    return !fecha.isBefore(inicio) && fecha.isBefore(fin);
  }

  public void cancelar(LocalDateTime fecha) {
    if (fin.isAfter(fecha)) fin = fecha;
  }
}
