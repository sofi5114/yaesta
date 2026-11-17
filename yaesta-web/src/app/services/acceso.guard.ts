import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const sesionGuard: CanActivateFn = (_ruta, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.usuario() ? true : router.createUrlTree(['/login'], {
    queryParams: { volver: estado.url }
  });
};

export const clienteGuard: CanActivateFn = (_ruta, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.usuario()) return router.createUrlTree(['/login'], { queryParams: { volver: estado.url } });
  return auth.usuario()?.rol === 'CLIENTE' ? true : router.createUrlTree(['/']);
};

export const negocioGuard: CanActivateFn = (_ruta, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.usuario()) return router.createUrlTree(['/login'], { queryParams: { volver: estado.url } });
  return auth.usuario()?.rol === 'NEGOCIO' ? true : router.createUrlTree(['/']);
};
