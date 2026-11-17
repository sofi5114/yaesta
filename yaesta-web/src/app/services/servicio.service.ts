import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs';
import { Respuesta } from '../models/negocio';
import { Pagina, Servicio } from '../models/servicio';

export interface ServicioRequest {
  nombre: string;
  precio: number;
  duracionMinutos: number;
  diasRecordatorio: number | null;
}

@Injectable({ providedIn: 'root' })
export class ServicioService {
  private readonly http = inject(HttpClient);
  listar(negocioId: number, pagina = 0) {
    return this.http.get<Respuesta<Pagina<Servicio>>>(
      `/api/negocios/${negocioId}/servicios`, { params: { pagina, tamano: 10 } }
    ).pipe(map(respuesta => respuesta.data));
  }
  crear(negocioId: number, datos: ServicioRequest) {
    return this.http.post<Respuesta<Servicio>>(
      `/api/negocios/${negocioId}/servicios`, datos
    ).pipe(map(respuesta => respuesta.data));
  }
  actualizar(negocioId: number, id: number, datos: ServicioRequest) {
    return this.http.put<Respuesta<Servicio>>(
      `/api/negocios/${negocioId}/servicios/${id}`, datos
    ).pipe(map(respuesta => respuesta.data));
  }
  desactivar(negocioId: number, id: number) {
    return this.http.delete<void>(`/api/negocios/${negocioId}/servicios/${id}`);
  }
}
