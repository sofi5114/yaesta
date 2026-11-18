import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs';
import { Respuesta } from '../models/negocio';
import { Pagina } from '../models/servicio';
export interface EstadoPlus {
  activa: boolean;
  demostracion: boolean;
  permitePrueba: boolean;
  venceEn: string | null;
  seguidores: number;
  siguiendo: boolean;
  alertas: boolean;
}
export interface Novedad {
  id: number;
  negocioId: number;
  negocio: string;
  titulo: string;
  mensaje: string;
  creadaEn: string;
  venceEn: string;
}
export interface Intercambio {
  id: number;
  origenId: number;
  destinoId: number;
  recibido: boolean;
  estado: string;
  venceEn: string;
}
export interface Alternativa {
  negocioId: number;
  servicioId: number;
  negocio: string;
  servicio: string;
  distanciaKm: number;
  esperaMinutos: number;
  duracionMinutos: number;
  precio: number;
  precioFinal: number;
  descuento: number;
  actualizadaEn: string;
}
export interface Ubicacion {
  latitud: number | null;
  longitud: number | null;
  descuento: number;
  cupos: number;
  actualizadaEn: string | null;
}
@Injectable({ providedIn: 'root' })
export class PlusService {
  private http = inject(HttpClient);
  get<T>(url: string) {
    return this.http.get<Respuesta<T>>(url).pipe(map((r) => r.data));
  }
  post<T>(url: string, body: unknown = {}) {
    return this.http.post<Respuesta<T>>(url, body).pipe(map((r) => r.data));
  }
  patch<T>(url: string, body: unknown) {
    return this.http.patch<Respuesta<T>>(url, body).pipe(map((r) => r.data));
  }
  delete<T>(url: string) {
    return this.http.delete<Respuesta<T>>(url).pipe(map((r) => r.data));
  }
  estado(id?: number) {
    return this.get<EstadoPlus>('/api/plus/membresia' + this.query(id));
  }
  prueba(id?: number) {
    return this.post<EstadoPlus>('/api/plus/prueba' + this.query(id));
  }
  cancelar(id?: number) {
    return this.delete<EstadoPlus>('/api/plus/membresia' + this.query(id));
  }
  seguir(id: number, alertas: boolean) {
    return this.http
      .put<Respuesta<EstadoPlus>>(`/api/plus/negocios/${id}/seguir?alertas=${alertas}`, {})
      .pipe(map((r) => r.data));
  }
  private query(id?: number) {
    return id ? '?negocioId=' + id : '';
  }
}
