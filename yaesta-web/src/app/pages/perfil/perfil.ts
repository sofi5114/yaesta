import { VistaCabecera } from '../../shared/vista-cabecera';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../services/auth.service';
import { PerfilService } from '../../services/perfil.service';

@Component({
  selector: 'app-perfil',
  imports: [VistaCabecera, FormsModule],
  templateUrl: './perfil.html',
  styleUrl: './perfil.css',
})
export class Perfil {
  readonly auth = inject(AuthService);
  private readonly servicio = inject(PerfilService);

  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly mensaje = signal('');
  readonly error = signal('');

  aceptaRecordatorios = false;

  constructor() {
    this.servicio.consultar().subscribe({
      next: (usuario) => {
        this.aceptaRecordatorios = usuario.aceptaRecordatorios;
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No pudimos consultar tu perfil. Si venció tu sesión, vuelve a ingresar.');
        this.cargando.set(false);
      },
    });
  }

  guardar() {
    if (this.guardando() || this.cargando() || this.auth.usuario()?.rol !== 'CLIENTE') {
      return;
    }

    this.guardando.set(true);
    this.error.set('');
    this.mensaje.set('');

    this.servicio.preferencias(this.aceptaRecordatorios).subscribe({
      next: (usuario) => {
        this.guardando.set(false);

        this.mensaje.set(
          usuario.aceptaRecordatorios
            ? 'Preferencia guardada. Se aplicará a tus próximas atenciones.'
            : 'Recordatorios desactivados. Los pendientes que aún podían cancelarse fueron cancelados.',
        );
      },
      error: () => {
        this.guardando.set(false);
        this.error.set('No pudimos guardar el cambio. Intenta nuevamente.');
      },
    });
  }
}
