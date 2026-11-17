import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, tap } from 'rxjs';
import { Usuario } from '../models/auth';
import { Respuesta } from '../models/negocio';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class PerfilService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  consultar() {
    return this.http.get<Respuesta<Usuario>>('/api/perfil').pipe(
      map(respuesta => respuesta.data),
      tap(usuario => this.auth.actualizarUsuario(usuario))
    );
  }

  preferencias(aceptaRecordatorios: boolean) {
    return this.http.patch<Respuesta<Usuario>>('/api/perfil/preferencias', {
      aceptaRecordatorios
    }).pipe(
      map(respuesta => respuesta.data),
      tap(usuario => this.auth.actualizarUsuario(usuario))
    );
  }
}
