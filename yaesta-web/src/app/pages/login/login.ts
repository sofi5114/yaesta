import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  correo = '';
  clave = '';

  readonly enviando = signal(false);
  readonly error = signal('');

  ingresar() {
    if (this.enviando()) {
      return;
    }

    this.enviando.set(true);
    this.error.set('');

    this.auth.ingresar(this.correo, this.clave).subscribe({
      next: () => {
        this.clave = '';
        this.enviando.set(false);
        const volver = this.ruta.snapshot.queryParamMap.get('volver') ?? '/';
        const interna =
          /^\/(?:$|turnos$|calendario$|perfil$|plus$|siguiendo$|negocios\/[1-9]\d*$|panel-negocio\/[1-9]\d*$|servicios-negocio\/[1-9]\d*$)/.test(
            volver,
          );
        void this.router.navigateByUrl(interna ? volver : '/');
      },
      error: (respuesta: HttpErrorResponse) => {
        this.enviando.set(false);
        this.clave = '';

        this.error.set(
          respuesta.status === 0 || respuesta.status >= 500
            ? 'No pudimos conectar con el servidor. Intenta nuevamente.'
            : 'No pudimos iniciar sesión. Revisa tu correo y contraseña.',
        );
      },
    });
  }
}
