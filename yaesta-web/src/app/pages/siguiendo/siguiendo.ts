import { VistaCabecera } from '../../shared/vista-cabecera';
import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PlusService, Novedad } from '../../services/plus.service';
import { Pagina } from '../../models/servicio';
@Component({
  selector: 'app-siguiendo',
  imports: [VistaCabecera, DatePipe, RouterLink],
  template: ` <main class="siguiendo">
    <a routerLink="/">← Explorar negocios</a>
    <app-vista-cabecera
      titulo="Mis negocios"
      etiqueta="CERCA DE TUS FAVORITOS"
      descripcion="Las novedades de los locales que eliges seguir. Seguir un negocio es gratis."
      [foto]="0"
    />
    @if (error()) {
      <section class="estado" role="alert">
        <h2>No pudimos cargar tus novedades</h2>
        <p>{{ error() }}</p>
        <button (click)="cargar()">Volver a intentar</button>
      </section>
    } @else if (cargando()) {
      <p role="status">Consultando novedades…</p>
    } @else {
      <div class="novedades">
        @for (n of resultado()?.contenido ?? []; track n.id) {
          <article>
            <span class="tipo">NOVEDAD DEL LOCAL</span
            ><a class="nombre" [routerLink]="['/negocios', n.negocioId]">{{ n.negocio }}</a>
            <h2>{{ n.titulo }}</h2>
            <p>{{ n.mensaje }}</p>
            <small>Disponible hasta {{ n.venceEn | date: 'dd/MM/yyyy HH:mm' }}</small
            ><a class="boton" [routerLink]="['/negocios', n.negocioId]">Ver local ↗</a>
          </article>
        } @empty {
          <section class="estado">
            <span class="reloj" aria-hidden="true">◷</span>
            <h2>Tus próximos favoritos empiezan aquí</h2>
            <p>
              Abre la ficha de un local y pulsa «Seguir negocio» cuando tenga su canal habilitado.
              Aquí verás sus publicaciones vigentes.
            </p>
            <a class="boton" routerLink="/">Descubrir negocios ↗</a>
          </section>
        }
      </div>
    }
    @if (resultado(); as p) {
      @if (p.totalPaginas > 1) {
        <nav aria-label="Páginas de novedades">
          <button [disabled]="cargando() || p.pagina === 0" (click)="cargar(p.pagina - 1)">
            Anterior</button
          ><span>{{ p.pagina + 1 }} de {{ p.totalPaginas }}</span
          ><button
            [disabled]="cargando() || p.pagina + 1 >= p.totalPaginas"
            (click)="cargar(p.pagina + 1)"
          >
            Siguiente
          </button>
        </nav>
      }
    }
  </main>`,
  styles: [
    `
      :host {
        display: block;
      }
      .siguiendo {
        max-width: 1100px;
        padding: 35px 24px 70px;
        margin: auto;
      }
      a {
        color: #9a4e34;
      }
      .etiqueta {
        margin-top: 35px;
        font-size: 10px;
        letter-spacing: 2px;
      }
      h1 {
        font-size: 44px;
        letter-spacing: -1.5px;
        margin: 12px 0;
      }
      .intro {
        color: #7b695b;
        line-height: 1.7;
      }
      .novedades {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 24px;
        margin-top: 35px;
      }
      article {
        background: #fffdfa;
        border: 1px solid #e9ddd3;
        border-radius: 30px;
        padding: 28px;
      }
      .tipo {
        font-size: 9px;
        letter-spacing: 1.5px;
        color: #9a6348;
      }
      .nombre {
        display: block;
        margin-top: 12px;
        font-weight: bold;
        text-decoration: none;
      }
      article p {
        white-space: pre-wrap;
        line-height: 1.7;
      }
      article small {
        display: block;
        color: #887263;
      }
      .boton,
      button {
        display: inline-block;
        background: #a54f35;
        color: white;
        padding: 13px 22px;
        border-radius: 28px;
        border: 0;
        text-decoration: none;
        margin-top: 20px;
      }
      .estado {
        grid-column: 1/-1;
        padding: 45px 25px;
        background: #f1e6dc;
        border-radius: 35px;
        text-align: center;
      }
      .estado p {
        max-width: 550px;
        margin: 12px auto;
        line-height: 1.7;
        color: #765f4e;
      }
      .reloj {
        font-size: 50px;
        color: #a54f35;
      }
      nav {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 20px;
      }
      button:disabled {
        opacity: 0.5;
      }
      @media (max-width: 650px) {
        .novedades {
          grid-template-columns: 1fr;
        }
        h1 {
          font-size: 36px;
        }
      }
    `,
  ],
})
export class Siguiendo {
  private api = inject(PlusService);
  readonly resultado = signal<Pagina<Novedad> | null>(null);
  readonly error = signal('');
  readonly cargando = signal(false);
  constructor() {
    this.cargar();
  }
  cargar(pagina = 0) {
    if (this.cargando()) return;
    this.cargando.set(true);
    this.error.set('');
    this.api.get<Pagina<Novedad>>('/api/plus/novedades?pagina=' + pagina).subscribe({
      next: (r) => {
        this.resultado.set(r);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(
          'Intenta nuevamente en unos momentos. Tus preferencias de seguimiento se conservarán.',
        );
        this.cargando.set(false);
      },
    });
  }
}
