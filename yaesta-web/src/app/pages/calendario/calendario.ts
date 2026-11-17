import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../services/auth.service';
import { RecordatorioService } from '../../services/recordatorio.service';
import { Recordatorio } from '../../models/recordatorio';

@Component({
  selector: 'app-calendario',
  imports: [DatePipe, RouterLink],
  templateUrl: './calendario.html',
  styleUrl: './calendario.css'
})
export class Calendario {
  readonly auth = inject(AuthService);
  private readonly servicio = inject(RecordatorioService);

  readonly mes = signal(
    new Date(new Date().getFullYear(), new Date().getMonth(), 1)
  );

  readonly recordatorios = signal<Recordatorio[]>([]);
  readonly fechaElegida = signal<string | null>(null);
  readonly cargando = signal(false);
  readonly cancelando = signal<number | null>(null);
  readonly confirmarId = signal<number | null>(null);
  readonly error = signal('');

  readonly semanas = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];

  readonly titulo = computed(() =>
    this.mes().toLocaleDateString('es-PE', {
      month: 'long',
      year: 'numeric'
    })
  );

  readonly dias = computed(() => {
    const fecha = this.mes();
    const inicio = (fecha.getDay() + 6) % 7;
    const cantidad = new Date(
      fecha.getFullYear(),
      fecha.getMonth() + 1,
      0
    ).getDate();

    const total = Math.ceil((inicio + cantidad) / 7) * 7;

    return Array.from({ length: total }, (_, indice) => {
      const numero = indice - inicio + 1;

      if (numero < 1 || numero > cantidad) {
        return null;
      }

      const clave = [
        fecha.getFullYear(),
        String(fecha.getMonth() + 1).padStart(2, '0'),
        String(numero).padStart(2, '0')
      ].join('-');

      return {
        numero,
        fecha: clave,
        cantidad: this.recordatorios().filter(
          item =>
            item.fechaProgramada === clave &&
            item.estado !== 'CANCELADO'
        ).length
      };
    });
  });

  readonly visibles = computed(() => {
    const fecha = this.fechaElegida();

    return this.recordatorios().filter(
      item => !fecha || item.fechaProgramada === fecha
    );
  });

  constructor() {
    if (this.auth.usuario()?.rol === 'CLIENTE') {
      this.cargar();
    }
  }

  cargar() {
    if (this.cargando() || this.cancelando() !== null) {
      return;
    }

    this.cargando.set(true);
    this.error.set('');

    const fecha = this.mes();

    this.servicio.calendario(
      fecha.getFullYear(),
      fecha.getMonth() + 1
    ).subscribe({
      next: datos => {
        this.recordatorios.set(datos);
        this.cargando.set(false);
      },
      error: (respuesta: HttpErrorResponse) => {
        this.recordatorios.set([]);
        this.error.set(this.mensajeError(respuesta));
        this.cargando.set(false);
      }
    });
  }

  cambiarMes(cambio: number) {
    if (this.cargando() || this.cancelando() !== null) {
      return;
    }

    const actual = this.mes();

    this.mes.set(new Date(
      actual.getFullYear(),
      actual.getMonth() + cambio,
      1
    ));

    this.fechaElegida.set(null);
    this.confirmarId.set(null);
    this.recordatorios.set([]);
    this.cargar();
  }

  cancelar(recordatorio: Recordatorio) {
    if (this.cancelando() !== null || this.cargando()) {
      return;
    }

    this.cancelando.set(recordatorio.id);
    this.error.set('');

    this.servicio.cancelar(recordatorio.id).subscribe({
      next: actualizado => {
        this.recordatorios.update(datos =>
          datos.map(item =>
            item.id === actualizado.id ? actualizado : item
          )
        );

        this.confirmarId.set(null);
        this.cancelando.set(null);
      },
      error: (respuesta: HttpErrorResponse) => {
        this.error.set(this.mensajeError(respuesta));
        this.cancelando.set(null);
      }
    });
  }

  puedeCancelar(recordatorio: Recordatorio) {
    return recordatorio.estado === 'PENDIENTE' ||
      recordatorio.estado === 'ERROR';
  }

  private mensajeError(respuesta: HttpErrorResponse): string {
    if (respuesta.status === 401) {
      return 'Tu sesión venció. Inicia sesión nuevamente.';
    }

    const mensaje = respuesta.error?.data?.mensaje;

    return typeof mensaje === 'string'
      ? mensaje
      : 'No pudimos completar la operación. Intenta nuevamente.';
  }
}