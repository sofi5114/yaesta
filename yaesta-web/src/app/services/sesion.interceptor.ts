import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const sesionInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const protegida = peticion.url.startsWith('/api/') &&
    !peticion.url.startsWith('/api/auth/');
  const enviada = protegida && token
    ? peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : peticion;

  return siguiente(enviada).pipe(catchError((error: HttpErrorResponse) => {
    if (protegida && token && error.status === 401 && auth.token() === token) {
      auth.cerrarSesion();
    }
    return throwError(() => error);
  }));
};
