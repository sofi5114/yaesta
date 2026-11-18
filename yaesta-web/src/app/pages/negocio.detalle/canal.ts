import { Component, Input, OnChanges, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { PlusService, EstadoPlus, Novedad } from '../../services/plus.service';
import { Pagina } from '../../models/servicio';
@Component({
  selector: 'app-canal',
  imports: [DatePipe, RouterLink],
  template: `
    @if (estado(); as e) {
      @if (e.activa || e.siguiendo) {
        <section class="canal">
          <h2>Novedades del negocio</h2>
          <p>Seguir este negocio es gratis.</p>
          @if (error()) {
            <p role="alert">{{ error() }}</p>
          }
          @if (auth.usuario()?.rol === 'CLIENTE') {
            @if (e.siguiendo) {
              <button [disabled]="ocupado()" (click)="seguir(false)">Dejar de seguir</button
              ><label
                ><input
                  type="checkbox"
                  [checked]="e.alertas"
                  [disabled]="ocupado() || !e.activa"
                  (change)="alertas($any($event.target).checked)"
                />
                Quiero recibir avisos cuando estén disponibles</label
              >
              <p>
                Consulta las publicaciones en <a routerLink="/siguiendo">Mis negocios</a>. Los
                avisos externos aún no están habilitados.
              </p>
            } @else {
              <button [disabled]="ocupado()" (click)="seguir(true)">Seguir negocio gratis</button>
            }
          }
          @for (p of publicaciones()?.contenido ?? []; track p.id) {
            <article>
              <h3>{{ p.titulo }}</h3>
              <p>{{ p.mensaje }}</p>
              <small>Hasta {{ p.venceEn | date: 'dd/MM/yyyy HH:mm' }}</small>
            </article>
          }
        </section>
      }
    }
  `,
  styles: [
    `
      .canal {
        margin: 24px 0;
        padding: 24px;
        background: #edf5f3;
        border-radius: 18px;
      }
      button {
        padding: 12px;
        border-radius: 10px;
        background: #a54f35;
        color: white;
        border: 0;
        margin: 8px 12px 8px 0;
      }
      article {
        border-top: 1px solid #c6dcd6;
        padding: 16px 0;
      }
      p {
        white-space: pre-wrap;
        line-height: 1.6;
      }
      label {
        display: block;
        margin-top: 12px;
      }
    `,
  ],
})
export class Canal implements OnChanges {
  @Input({ required: true }) negocioId!: number;
  readonly auth = inject(AuthService);
  private api = inject(PlusService);
  readonly estado = signal<EstadoPlus | null>(null);
  readonly publicaciones = signal<Pagina<Novedad> | null>(null);
  readonly error = signal('');
  readonly ocupado = signal(false);
  ngOnChanges() {
    if (!this.auth.usuario()) return;
    this.api.estado(this.negocioId).subscribe({ next: (e) => this.estado.set(e), error: () => {} });
    this.api
      .get<Pagina<Novedad>>(`/api/plus/negocios/${this.negocioId}/publicaciones`)
      .subscribe({ next: (p) => this.publicaciones.set(p), error: () => {} });
  }
  seguir(valor: boolean) {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    const o = valor
      ? this.api.seguir(this.negocioId, false)
      : this.api.delete<EstadoPlus>(`/api/plus/negocios/${this.negocioId}/seguir`);
    o.subscribe({
      next: (e) => {
        this.estado.set(e);
        this.ocupado.set(false);
      },
      error: (e) => {
        this.error.set(e.error?.data?.mensaje || 'No se pudo actualizar');
        this.ocupado.set(false);
      },
    });
  }
  alertas(valor: boolean) {
    this.ocupado.set(true);
    this.api.seguir(this.negocioId, valor).subscribe({
      next: (e) => {
        this.estado.set(e);
        this.ocupado.set(false);
      },
      error: () => {
        this.error.set('No se pudo guardar tu preferencia');
        this.ocupado.set(false);
      },
    });
  }
}
