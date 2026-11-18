import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../services/auth.service';
import { RegistroRequest } from '../../models/auth';

@Component({
  selector: 'app-registro',
  imports: [FormsModule, RouterLink],
  templateUrl: './registro.html',
  styleUrl: './registro.css',
})
export class Registro {
  private readonly auth = inject(AuthService);

  datos: RegistroRequest = {
    nombre: '',
    correo: '',
    clave: '',
    aceptaRecordatorios: false,
  };

  confirmarClave = '';

  readonly enviando = signal(false);
  readonly creado = signal(false);
  readonly error = signal('');

  registrar() {
    if (this.enviando() || this.creado()) {
      return;
    }

    this.error.set('');

    if (!this.datos.nombre.trim()) {
      this.error.set('Escribe tu nombre.');
      return;
    }

    if (this.datos.clave !== this.confirmarClave) {
      this.error.set('Las contraseñas no coinciden.');
      return;
    }

    this.enviando.set(true);

    this.auth.registrar(this.datos).subscribe({
      next: () => {
        this.datos.clave = '';
        this.confirmarClave = '';
        this.enviando.set(false);
        this.creado.set(true);
      },
      error: (respuesta: HttpErrorResponse) => {
        this.enviando.set(false);

        const mensaje = respuesta.error?.data?.mensaje;

        this.error.set(
          respuesta.status === 409
            ? 'No se pudo crear la cuenta con esos datos. Si ya tienes una, inicia sesión.'
            : typeof mensaje === 'string'
              ? mensaje
              : 'No pudimos crear tu cuenta. Revisa los datos e intenta nuevamente.',
        );
      },
    });
  }
}
