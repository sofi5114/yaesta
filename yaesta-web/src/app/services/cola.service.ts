import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { map } from 'rxjs';

import { AuthService } from './auth.service';
import { Respuesta } from '../models/negocio';
import { Pagina } from '../models/servicio';
import { Turno } from '../models/turno';

export interface ConfiguracionCola {
  negocioId: number;
  aceptaTurnos: boolean;
  toleranciaMinutos: number;
}

export type AccionCola =
  | 'llegada'
  | 'inicio'
  | 'finalizacion'
  | 'ausencia';

@Injectable({
  providedIn: 'root'
})
export class ColaService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  listar(negocioId: number, pagina = 0) {
    return this.http
      .get<Respuesta<Pagina<Turno>>>(
        `/api/negocios/${negocioId}/cola`,
        {
          headers: this.cabeceras(),
          params: { pagina, tamano: 10 }
        }
      )
      .pipe(map(respuesta => respuesta.data));
  }

  configuracion(negocioId: number) {
    return this.http
      .get<Respuesta<ConfiguracionCola>>(
        `/api/negocios/${negocioId}/cola/configuracion`,
        { headers: this.cabeceras() }
      )
      .pipe(map(respuesta => respuesta.data));
  }

  configurar(
    negocioId: number,
    aceptaTurnos: boolean,
    toleranciaMinutos: number
  ) {
    return this.http
      .patch<Respuesta<ConfiguracionCola>>(
        `/api/negocios/${negocioId}/cola/configuracion`,
        { aceptaTurnos, toleranciaMinutos },
        { headers: this.cabeceras() }
      )
      .pipe(map(respuesta => respuesta.data));
  }

  llamarSiguiente(negocioId: number) {
    return this.http
      .post<Respuesta<Turno>>(
        `/api/negocios/${negocioId}/cola/llamados`,
        {},
        { headers: this.cabeceras() }
      )
      .pipe(map(respuesta => respuesta.data));
  }

  gestionar(
    negocioId: number,
    turnoId: number,
    accion: AccionCola
  ) {
    return this.http
      .patch<Respuesta<Turno>>(
        `/api/negocios/${negocioId}/cola/turnos/${turnoId}/${accion}`,
        {},
        { headers: this.cabeceras() }
      )
      .pipe(map(respuesta => respuesta.data));
  }

  registrarPresencial(
    negocioId: number,
    servicioId: number,
    nombreCliente: string
  ) {
    return this.http
      .post<Respuesta<Turno>>(
        `/api/negocios/${negocioId}/cola/presenciales`,
        {
          servicioId,
          nombreCliente: nombreCliente.trim()
        },
        { headers: this.cabeceras() }
      )
      .pipe(map(respuesta => respuesta.data));
  }

  private cabeceras() {
    const token = this.auth.token();

    return token
      ? new HttpHeaders({ Authorization: `Bearer ${token}` })
      : new HttpHeaders();
  }
}