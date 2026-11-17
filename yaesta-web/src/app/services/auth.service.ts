import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, tap } from 'rxjs';
import { LoginResponse, RegistroRequest, Usuario } from '../models/auth';
import { Respuesta } from '../models/negocio';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly clave = 'yaesta.sesion';
  private readonly sesion = signal<LoginResponse | null>(this.recuperar());
  private vencimiento?: ReturnType<typeof setTimeout>;

  readonly usuario = () => this.sesion()?.usuario ?? null;
  readonly token = () => this.sesion()?.token ?? null;

  constructor() {
    this.programarVencimiento();
  }

  ingresar(correo: string, clave: string) {
    return this.http.post<Respuesta<LoginResponse>>('/api/auth/login', {
      correo: correo.trim(), clave
    }).pipe(
      map(respuesta => respuesta.data),
      tap(datos => this.guardar(datos))
    );
  }

  registrar(datos: RegistroRequest) {
    return this.http.post<Respuesta<Usuario>>('/api/auth/registro', {
      ...datos, nombre: datos.nombre.trim(), correo: datos.correo.trim()
    }).pipe(map(respuesta => respuesta.data));
  }

  actualizarUsuario(usuario: Usuario) {
    const actual = this.sesion();
    if (actual) this.guardar({ ...actual, usuario });
  }

  cerrarSesion() {
    clearTimeout(this.vencimiento);
    this.sesion.set(null);
    try { sessionStorage.removeItem(this.clave); } catch { /* Sesión en memoria. */ }
  }

  private guardar(datos: LoginResponse) {
    this.sesion.set(datos);
    try { sessionStorage.setItem(this.clave, JSON.stringify(datos)); }
    catch { /* Si el navegador bloquea el almacenamiento, se conserva en memoria. */ }
    this.programarVencimiento();
  }

  private recuperar(): LoginResponse | null {
    try {
      const guardado = sessionStorage.getItem('yaesta.sesion');
      if (!guardado) return null;
      const datos = JSON.parse(guardado) as LoginResponse;
      if (!datos.usuario || !['CLIENTE', 'NEGOCIO'].includes(datos.usuario.rol) ||
          this.expira(datos.token) <= Date.now()) {
        sessionStorage.removeItem('yaesta.sesion');
        return null;
      }
      return datos;
    } catch {
      return null;
    }
  }

  private expira(token: string): number {
    try {
      const parte = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(parte.padEnd(Math.ceil(parte.length / 4) * 4, '=')));
      return typeof payload.exp === 'number' ? payload.exp * 1000 : 0;
    } catch { return 0; }
  }

  private programarVencimiento() {
    clearTimeout(this.vencimiento);
    const token = this.token();
    if (!token) return;
    const restante = this.expira(token) - Date.now();
    if (restante <= 0) { this.cerrarSesion(); return; }
    this.vencimiento = setTimeout(() => this.cerrarSesion(), Math.min(restante, 2147483647));
  }
}
