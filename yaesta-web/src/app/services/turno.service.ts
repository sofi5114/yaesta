import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { map } from 'rxjs';

import { AuthService } from './auth.service';
import { Respuesta } from '../models/negocio';
import { Pagina } from '../models/servicio';
import { Turno } from '../models/turno';

@Injectable({
  providedIn: 'root'
})
export class TurnoService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  solicitar(negocioId: number, servicioId: number) {
    return this.http
      .post<Respuesta<Turno>>(
        `/api/negocios/${negocioId}/turnos`,
        { servicioId },
        { headers: this.cabeceras() }
      )
      .pipe(map(respuesta => respuesta.data));
  }

  listar(pagina = 0) {
    return this.http
      .get<Respuesta<Pagina<Turno>>>('/api/turnos', {
        headers: this.cabeceras(),
        params: { pagina, tamano: 10 }
      })
      .pipe(map(respuesta => respuesta.data));
  }

  cancelar(negocioId: number, turnoId: number) {
    return this.http
      .delete<Respuesta<Turno>>(
        `/api/negocios/${negocioId}/turnos/${turnoId}`,
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