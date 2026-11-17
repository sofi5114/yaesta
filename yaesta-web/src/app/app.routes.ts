import { Routes } from '@angular/router';
import { clienteGuard, negocioGuard, sesionGuard } from './services/acceso.guard';

export const routes: Routes = [
  { path:'', pathMatch:'full', children:[] },
  { path:'negocios/:id', loadComponent:() => import('./pages/negocio.detalle/negocio-detalle').then(m => m.NegocioDetalle) },
  { path:'login', loadComponent:() => import('./pages/login/login').then(m => m.Login) },
  { path:'registro', loadComponent:() => import('./pages/registro/registro').then(m => m.Registro) },
  { path:'turnos', canActivate:[clienteGuard], loadComponent:() => import('./pages/mis-turnos/mis-turnos').then(m => m.MisTurnos) },
  { path:'calendario', canActivate:[clienteGuard], loadComponent:() => import('./pages/calendario/calendario').then(m => m.Calendario) },
  { path:'perfil', canActivate:[sesionGuard], loadComponent:() => import('./pages/perfil/perfil').then(m => m.Perfil) },
  { path:'panel-negocio/:id', canActivate:[negocioGuard], loadComponent:() => import('./pages/panel-negocio/panel-negocio').then(m => m.PanelNegocio) },
  { path:'servicios-negocio/:id', canActivate:[negocioGuard], loadComponent:() => import('./pages/servicios-negocio/servicios-negocio').then(m => m.ServiciosNegocio) },
  { path:'**', redirectTo:'' }
];
