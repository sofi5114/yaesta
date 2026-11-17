import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { map } from 'rxjs';

import { AuthService } from './auth.service';
import { Respuesta } from '../models/negocio';
import { Recordatorio } from '../models/recordatorio';

@Injectable({
  providedIn: 'root'
})
export class RecordatorioService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  calendario(anio: number, mes: number) {
    return this.http
      .get<Respuesta<Recordatorio[]>>(
        '/api/recordatorios/calendario',
        {
          headers: this.cabeceras(),
          params: { anio, mes }
        }
      )
      .pipe(map(respuesta => respuesta.data));
  }

  cancelar(id: number) {
    return this.http
      .delete<Respuesta<Recordatorio>>(
        `/api/recordatorios/${id}`,
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