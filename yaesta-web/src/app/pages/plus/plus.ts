import { VistaCabecera } from '../../shared/vista-cabecera';
import { Component, inject, signal } from '@angular/core';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { NegocioService } from '../../services/negocio.service';
import { Negocio } from '../../models/negocio';
import { Pagina } from '../../models/servicio';
import {
  PlusService,
  EstadoPlus,
  Novedad,
  Intercambio,
  Ubicacion,
} from '../../services/plus.service';
@Component({
  selector: 'app-plus',
  imports: [VistaCabecera, FormsModule, RouterLink, DatePipe, CurrencyPipe],
  templateUrl: './plus.html',
  styleUrl: './plus.css',
})
export class Plus {
  readonly auth = inject(AuthService);
  readonly api = inject(PlusService);
  private negociosApi = inject(NegocioService);
  private ruta = inject(ActivatedRoute);
  readonly planes = signal<{
    clienteMensual: number;
    negocioMensual: number | null;
    moneda: string;
    cobrosHabilitados: boolean;
    saldoHabilitado: boolean;
  } | null>(null);
  readonly estado = signal<EstadoPlus | null>(null);
  readonly negocios = signal<Negocio[]>([]);
  readonly publicaciones = signal<Pagina<Novedad> | null>(null);
  readonly propuestas = signal<Intercambio[]>([]);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly ocupado = signal(false);
  private carga = 0;
  negocioId: number | undefined;
  titulo = '';
  texto = '';
  dias = 7;
  origenId: number | null = null;
  destinoId: number | null = null;
  latitud: number | null = null;
  longitud: number | null = null;
  descuento = 0;
  cupos = 0;
  confirmarCancelacion = false;
  constructor() {
    this.api
      .get<{
        clienteMensual: number;
        negocioMensual: number | null;
        moneda: string;
        cobrosHabilitados: boolean;
        saldoHabilitado: boolean;
      }>('/api/plus/planes')
      .subscribe({ next: (p) => this.planes.set(p), error: (e) => this.fallo(e) });
    if (this.auth.usuario()?.rol === 'NEGOCIO') {
      this.negociosApi.misNegocios().subscribe({
        next: (n) => {
          this.negocios.set(n);
          const id = Number(this.ruta.snapshot.queryParamMap.get('negocioId'));
          this.negocioId = n.some((x) => x.id === id) ? id : n[0]?.id;
          if (this.negocioId) this.cargar();
        },
        error: (e) => this.fallo(e),
      });
    } else this.cargar();
  }
  cargar() {
    const carga = ++this.carga;
    this.error.set('');
    this.estado.set(null);
    this.api.estado(this.negocioId).subscribe({
      next: (e) => {
        if (carga === this.carga) this.estado.set(e);
      },
      error: (e) => {
        if (carga === this.carga) this.fallo(e);
      },
    });
    if (this.auth.usuario()?.rol === 'NEGOCIO' && this.negocioId) {
      this.cargarPublicaciones();
      this.api.get<Ubicacion>(`/api/negocios/${this.negocioId}/cola/ubicacion`).subscribe({
        next: (u) => {
          if (carga !== this.carga) return;
          this.latitud = u.latitud;
          this.longitud = u.longitud;
          this.descuento = u.descuento;
          this.cupos = u.cupos;
        },
        error: (e) => this.fallo(e),
      });
    } else {
      this.cargarPublicaciones();
      this.cargarIntercambios();
    }
  }
  cargarPublicaciones(pagina = 0) {
    const id = this.negocioId;
    const url = this.negocioId
      ? `/api/plus/negocios/${this.negocioId}/publicaciones`
      : '/api/plus/novedades';
    this.api.get<Pagina<Novedad>>(url + '?pagina=' + pagina).subscribe({
      next: (p) => {
        if (id === this.negocioId) this.publicaciones.set(p);
      },
      error: (e) => this.fallo(e),
    });
  }
  cargarIntercambios() {
    this.api
      .get<Intercambio[]>('/api/plus/intercambios')
      .subscribe({ next: (i) => this.propuestas.set(i), error: (e) => this.fallo(e) });
  }
  ejecutar<T>(operacion: Observable<T>, mensaje: string) {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.error.set('');
    this.mensaje.set('');
    operacion.subscribe({
      next: () => {
        this.ocupado.set(false);
        this.mensaje.set(mensaje);
        this.confirmarCancelacion = false;
        this.cargar();
      },
      error: (e) => {
        this.ocupado.set(false);
        this.fallo(e);
      },
    });
  }
  prueba() {
    this.ejecutar(
      this.api.prueba(this.negocioId),
      'Prueba activada por 30 días. No se realizó ningún cobro.',
    );
  }
  cancelar() {
    this.ejecutar(this.api.cancelar(this.negocioId), 'Prueba finalizada.');
  }
  publicar() {
    if (!this.titulo.trim() || !this.texto.trim()) return;
    this.ejecutar(
      this.api.post(`/api/plus/negocios/${this.negocioId}/publicaciones`, {
        titulo: this.titulo,
        mensaje: this.texto,
        vigenciaDias: this.dias,
      }),
      'Novedad publicada.',
    );
  }
  borrar(id: number) {
    this.ejecutar(
      this.api.delete(`/api/plus/negocios/${this.negocioId}/publicaciones/${id}`),
      'Publicación retirada.',
    );
  }
  proponer() {
    if (!this.origenId || !this.destinoId) return;
    this.ejecutar(
      this.api.post('/api/plus/intercambios', {
        origenId: this.origenId,
        destinoId: this.destinoId,
      }),
      'Propuesta enviada. La otra persona tiene tres minutos para responder.',
    );
  }
  resolver(id: number, aceptar: boolean) {
    this.ejecutar(
      this.api.patch('/api/plus/intercambios/' + id, { aceptar }),
      'Propuesta actualizada.',
    );
  }
  guardarUbicacion() {
    if (this.latitud === null || this.longitud === null) return;
    this.ejecutar(
      this.api.patch(`/api/negocios/${this.negocioId}/cola/ubicacion`, {
        latitud: this.latitud,
        longitud: this.longitud,
        descuento: this.descuento,
        cupos: this.cupos,
      }),
      'Disponibilidad confirmada y promoción guardada.',
    );
  }
  ubicar() {
    if (!navigator.geolocation) {
      this.error.set('Escribe las coordenadas del local.');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (p) => {
        this.latitud = p.coords.latitude;
        this.longitud = p.coords.longitude;
      },
      () => this.error.set('No se pudo obtener la ubicación. Puedes escribirla manualmente.'),
    );
  }
  fallo(e: HttpErrorResponse) {
    this.error.set(
      e.error?.data?.mensaje || 'No pudimos completar la operación. Intenta otra vez.',
    );
  }
}
