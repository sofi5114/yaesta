import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { map } from 'rxjs';

import { Negocio, Respuesta } from '../models/negocio';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class NegocioService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  private readonly url = '/api/negocios';

  listar() {
    return this.http
      .get<Respuesta<Negocio[]>>(this.url)
      .pipe(map(respuesta => respuesta.data));
  }

  misNegocios() {
    const token = this.auth.token();

    const headers = token
      ? new HttpHeaders({ Authorization: `Bearer ${token}` })
      : new HttpHeaders();

    return this.http
      .get<Respuesta<Negocio[]>>(
        `${this.url}/propios`,
        { headers }
      )
      .pipe(map(respuesta => respuesta.data));
  }

  buscar(id: number) {
    return this.http
      .get<Respuesta<Negocio>>(`${this.url}/${id}`)
      .pipe(map(respuesta => respuesta.data));
  }
}