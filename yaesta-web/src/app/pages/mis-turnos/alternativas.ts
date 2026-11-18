import { Component, Input, Output, EventEmitter, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PlusService, Alternativa } from '../../services/plus.service';
@Component({
  selector: 'app-alternativas',
  imports: [FormsModule, CurrencyPipe],
  template: `
    <section class="alternativas">
      <h3>¿No puedes esperar?</h3>
      <p>
        Encuentra otra opción cercana para tu servicio. Revisar opciones
        conserva tu turno.
      </p>
      <button [disabled]="ocupado()" (click)="ubicar()">Buscar cerca de mí</button>
      <details>
        <summary>Elegir ubicación manualmente</summary>
        <label
          >Latitud<input type="number" step="any" [(ngModel)]="latitud" min="-90" max="90" /></label
        ><label
          >Longitud<input
            type="number"
            step="any"
            [(ngModel)]="longitud"
            min="-180"
            max="180" /></label
        ><button [disabled]="ocupado() || latitud === null || longitud === null" (click)="buscar()">
          Buscar aquí
        </button>
      </details>
      @if (error()) {
        <p role="alert">{{ error() }}</p>
      }
      @if (buscado() && !opciones().length) {
        <p>
          No encontramos alternativas con disponibilidad reciente y espera conocida. Conservas tu
          turno.
        </p>
      }
      @for (a of opciones(); track a.servicioId) {
        <article>
          <h4>{{ a.negocio }}</h4>
          <p>{{ a.servicio }} · {{ a.distanciaKm }} km en línea recta</p>
          <p>
            Espera aproximada: {{ a.esperaMinutos }} min · Atención: {{ a.duracionMinutos }} min
          </p>
          <strong>{{ a.precioFinal | currency: 'PEN' : 'S/ ' }}</strong>
          @if (a.descuento) {
            <span> · {{ a.descuento }} % de beneficio por demora</span>
          }
          @if (elegida()?.servicioId === a.servicioId) {
            <p>
              Confirma el tiempo que necesitas para llegar: el negocio no te llamará antes de que
              termine ese plazo. Después se aplica su cola y tolerancia. Al confirmar se cancelará
              tu turno actual. Precio aceptado: {{ a.precioFinal | currency: 'PEN' : 'S/ ' }}.
            </p>
            <label
              >¿Cuántos minutos necesitas para llegar?
              <input type="number" min="1" max="60" [(ngModel)]="minutosTraslado"
            /></label>
            <button
              [disabled]="ocupado() || minutosTraslado < 1 || minutosTraslado > 60"
              (click)="cambiar(a)"
            >
              Confirmar cambio de negocio</button
            ><button [disabled]="ocupado()" (click)="elegida.set(null)">Conservar mi turno</button>
          } @else {
            <button [disabled]="ocupado()" (click)="elegir(a)">Revisar cambio</button>
          }
        </article>
      }
      <p class="nota">
        Las distancias no son rutas de viaje. Confirma el servicio y las condiciones del local. El
        beneficio se fija al confirmar y no se combina con saldo.
      </p>
    </section>
  `,
  styles: [
    `
      .alternativas {
        margin-top: 20px;
        padding: 18px;
        background: #f0f6f4;
        border-radius: 14px;
      }
      button {
        padding: 11px 14px;
        margin: 8px 8px 8px 0;
        border: 0;
        border-radius: 8px;
        background: #a54f35;
        color: white;
      }
      button:disabled {
        opacity: 0.5;
      }
      article {
        border-top: 1px solid #c3d8d1;
        margin-top: 16px;
        padding-top: 14px;
      }
      label {
        display: block;
        margin: 10px 0;
      }
      input {
        padding: 8px;
        max-width: 160px;
        margin-left: 8px;
      }
      .nota {
        font-size: 12px;
      }
      p {
        line-height: 1.6;
      }
    `,
  ],
})
export class Alternativas {
  @Input({ required: true }) turnoId!: number;
  @Output() cambiado = new EventEmitter<void>();
  private api = inject(PlusService);
  readonly opciones = signal<Alternativa[]>([]);
  readonly elegida = signal<Alternativa | null>(null);
  readonly error = signal('');
  readonly ocupado = signal(false);
  readonly buscado = signal(false);
  minutosTraslado = 10;
  latitud: number | null = null;
  longitud: number | null = null;
  ubicar() {
    if (!navigator.geolocation) {
      this.error.set('Selecciona una ubicación manualmente.');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (p) => {
        this.latitud = p.coords.latitude;
        this.longitud = p.coords.longitude;
        this.buscar();
      },
      () => this.error.set('No pudimos acceder a tu ubicación. Puedes introducirla manualmente.'),
    );
  }
  buscar() {
    if (this.latitud === null || this.longitud === null || this.ocupado()) return;
    this.ocupado.set(true);
    this.error.set('');
    this.elegida.set(null);
    this.api
      .get<Alternativa[]>(
        `/api/turnos/${this.turnoId}/alternativas?latitud=${this.latitud}&longitud=${this.longitud}`,
      )
      .subscribe({
        next: (a) => {
          this.opciones.set(a);
          this.buscado.set(true);
          this.ocupado.set(false);
        },
        error: (e) => {
          this.error.set(e.error?.data?.mensaje || 'No se pudo consultar');
          this.ocupado.set(false);
        },
      });
  }
  elegir(a: Alternativa) {
    this.minutosTraslado = Math.min(60, Math.max(1, Math.ceil(a.distanciaKm * 4)));
    this.elegida.set(a);
  }
  cambiar(a: Alternativa) {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.error.set('');
    this.api
      .post(`/api/turnos/${this.turnoId}/derivacion`, {
        servicioId: a.servicioId,
        precioAceptado: a.precioFinal,
        minutosTraslado: this.minutosTraslado,
      })
      .subscribe({
        next: () => {
          this.opciones.set([]);
          this.elegida.set(null);
          this.ocupado.set(false);
          this.cambiado.emit();
        },
        error: (e) => {
          this.error.set(
            e.error?.data?.mensaje ||
              'No se pudo cambiar. Conservas tu turno si la operación no fue confirmada. Actualiza Mis turnos antes de reintentar.',
          );
          this.ocupado.set(false);
        },
      });
  }
}
