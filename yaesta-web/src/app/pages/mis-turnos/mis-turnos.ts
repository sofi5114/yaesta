import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { timer } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../services/auth.service';
import { TurnoService } from '../../services/turno.service';
import { Pagina } from '../../models/servicio';
import { Turno } from '../../models/turno';

@Component({
  selector: 'app-mis-turnos',
  imports: [RouterLink],
  templateUrl: './mis-turnos.html',
  styleUrl: './mis-turnos.css'
})
export class MisTurnos {
  readonly auth = inject(AuthService);
  private readonly servicio = inject(TurnoService);

  readonly resultado = signal<Pagina<Turno> | null>(null);
  readonly cargando = signal(false);
  readonly error = signal('');
  readonly confirmarId = signal<number | null>(null);
  readonly cancelando = signal(false);

  readonly estados: Record<Turno['estado'], string> = {
    EN_ESPERA: 'En espera',
    LLAMADO: 'Te están llamando',
    ATENDIENDO: 'En atención',
    ATENDIDO: 'Atendido',
    CANCELADO: 'Cancelado',
    AUSENTE: 'Ausente'
  };

  constructor() {
    timer(15000, 15000).pipe(takeUntilDestroyed()).subscribe(() => {
      if (this.auth.usuario()?.rol === 'CLIENTE' && document.visibilityState === 'visible') this.cargar();
    });
    if (this.auth.usuario()?.rol === 'CLIENTE') {
      this.cargar();
    }
  }

  cargar(pagina = this.resultado()?.pagina ?? 0) {
    if (this.cargando() || this.cancelando()) {
      return;
    }

    this.cargando.set(true);
    this.error.set('');

    this.servicio.listar(pagina).subscribe({
      next: datos => {
        this.resultado.set(datos);
        this.cargando.set(false);
      },
      error: (respuesta: HttpErrorResponse) => {
        this.error.set(this.mensajeError(respuesta));
        this.cargando.set(false);
      }
    });
  }

  cancelar(turno: Turno) {
    if (this.cancelando() || this.cargando()) {
      return;
    }

    this.cancelando.set(true);
    this.error.set('');

    this.servicio.cancelar(turno.negocioId, turno.id).subscribe({
      next: actualizado => {
        this.resultado.update(datos =>
          datos
            ? {
                ...datos,
                contenido: datos.contenido.map(item =>
                  item.id === actualizado.id ? actualizado : item
                )
              }
            : null
        );

        this.confirmarId.set(null);
        this.cancelando.set(false);
      },
      error: (respuesta: HttpErrorResponse) => {
        this.error.set(this.mensajeError(respuesta));
        this.cancelando.set(false);
      }
    });
  }

  puedeCancelar(turno: Turno) {
    return turno.estado === 'EN_ESPERA' || turno.estado === 'LLAMADO';
  }

  private mensajeError(respuesta: HttpErrorResponse): string {
    if (respuesta.status === 401) {
      return 'Tu sesión venció. Inicia sesión nuevamente.';
    }

    const mensaje = respuesta.error?.data?.mensaje;

    return typeof mensaje === 'string'
      ? mensaje
      : 'No pudimos completar la operación. Intenta nuevamente.';
  }
}
