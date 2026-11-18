import { fotoCategoria } from '../../shared/categorias-visuales';
import { VistaCabecera } from '../../shared/vista-cabecera';
import { Canal } from './canal';
import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Negocio } from '../../models/negocio';
import { Pagina, Servicio } from '../../models/servicio';
import { NegocioService } from '../../services/negocio.service';
import { ServicioService } from '../../services/servicio.service';

import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../services/auth.service';
import { TurnoService } from '../../services/turno.service';
import { Turno } from '../../models/turno';

@Component({
  selector: 'app-negocio-detalle',
  imports: [VistaCabecera, CurrencyPipe, RouterLink, Canal],
  templateUrl: './negocio-detalle.html',
  styleUrl: './negocio-detalle.css',
})
export class NegocioDetalle {
  readonly fotoCategoria = fotoCategoria;
  private readonly ruta = inject(ActivatedRoute);
  private readonly negocios = inject(NegocioService);
  private readonly servicios = inject(ServicioService);

  private readonly turnos = inject(TurnoService);

  readonly negocio = signal<Negocio | null>(null);
  readonly resultado = signal<Pagina<Servicio> | null>(null);
  readonly seleccionado = signal<Servicio | null>(null);
  readonly cargando = signal(true);
  readonly cambiandoPagina = signal(false);
  readonly error = signal('');
  readonly errorPagina = signal('');
  readonly auth = inject(AuthService);
  readonly solicitando = signal(false);
  readonly errorTurno = signal('');
  readonly turnoCreado = signal<Turno | null>(null);

  constructor() {
    this.cargar();
  }

  cargar() {
    const id = Number(this.ruta.snapshot.paramMap.get('id'));

    if (!Number.isSafeInteger(id) || id <= 0) {
      this.error.set('El negocio no es válido.');
      this.cargando.set(false);
      return;
    }

    this.cargando.set(true);
    this.error.set('');

    forkJoin({
      negocio: this.negocios.buscar(id),
      servicios: this.servicios.listar(id),
    }).subscribe({
      next: (datos) => {
        this.negocio.set(datos.negocio);
        this.resultado.set(datos.servicios);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No pudimos abrir este negocio. Revisa tu conexión e intenta nuevamente.');
        this.cargando.set(false);
      },
    });
  }

  cambiarPagina(pagina: number) {
    const negocio = this.negocio();
    const resultado = this.resultado();

    if (
      !negocio ||
      !resultado ||
      this.cambiandoPagina() ||
      pagina < 0 ||
      pagina >= resultado.totalPaginas
    ) {
      return;
    }

    this.cambiandoPagina.set(true);
    this.errorPagina.set('');

    this.servicios.listar(negocio.id, pagina).subscribe({
      next: (datos) => {
        this.resultado.set(datos);
        this.cambiandoPagina.set(false);
      },
      error: () => {
        this.errorPagina.set('No pudimos cargar esa página.');
        this.cambiandoPagina.set(false);
      },
    });
  }
  sacarTurno() {
    const negocio = this.negocio();
    const servicio = this.seleccionado();

    if (!negocio || !servicio || this.solicitando() || this.turnoCreado()) {
      return;
    }

    if (this.auth.usuario()?.rol !== 'CLIENTE') {
      this.errorTurno.set('Inicia sesión con una cuenta de cliente para sacar turno.');
      return;
    }

    this.solicitando.set(true);
    this.errorTurno.set('');

    this.turnos.solicitar(negocio.id, servicio.id).subscribe({
      next: (turno) => {
        this.turnoCreado.set(turno);
        this.solicitando.set(false);
      },
      error: (respuesta: HttpErrorResponse) => {
        this.solicitando.set(false);

        const mensaje = respuesta.error?.data?.mensaje;

        this.errorTurno.set(
          respuesta.status === 401
            ? 'Tu sesión venció. Inicia sesión nuevamente.'
            : typeof mensaje === 'string'
              ? mensaje
              : 'No pudimos registrar el turno. Intenta nuevamente.',
        );
      },
    });
  }
}
