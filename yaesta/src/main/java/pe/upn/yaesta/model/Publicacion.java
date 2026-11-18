package pe.upn.yaesta.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "publicacion")
public class Publicacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long negocioId;

  @Column(nullable = false, length = 100, columnDefinition = "nvarchar(100)")
  private String titulo;

  @Column(nullable = false, length = 1000, columnDefinition = "nvarchar(1000)")
  private String mensaje;

  @Column(nullable = false)
  private LocalDateTime creadaEn;

  @Column(nullable = false)
  private LocalDateTime venceEn;

  public Publicacion(Long negocioId, String titulo, String mensaje, LocalDateTime fecha, int dias) {
    this.negocioId = negocioId;
    this.titulo = titulo.trim();
    this.mensaje = mensaje.trim();
    creadaEn = fecha;
    venceEn = fecha.plusDays(dias);
  }
}
