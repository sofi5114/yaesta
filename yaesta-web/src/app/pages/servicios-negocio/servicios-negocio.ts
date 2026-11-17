import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { NegocioService } from '../../services/negocio.service';
import { ColaService } from '../../services/cola.service';
import { ServicioRequest, ServicioService } from '../../services/servicio.service';
import { Negocio } from '../../models/negocio';
import { Pagina, Servicio } from '../../models/servicio';

@Component({
  selector:'app-servicios-negocio', imports:[FormsModule, CurrencyPipe, RouterLink],
  templateUrl:'./servicios-negocio.html', styleUrl:'./servicios-negocio.css'
})
export class ServiciosNegocio {
  private readonly ruta = inject(ActivatedRoute);
  private readonly negocios = inject(NegocioService);
  private readonly cola = inject(ColaService);
  private readonly servicio = inject(ServicioService);
  readonly id = Number(this.ruta.snapshot.paramMap.get('id'));
  readonly negocio = signal<Negocio | null>(null);
  readonly resultado = signal<Pagina<Servicio> | null>(null);
  readonly cargando = signal(false);
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly editarId = signal<number | null>(null);
  readonly confirmarId = signal<number | null>(null);
  datos: ServicioRequest = this.vacio();

  constructor() { this.cargar(0); }

  cargar(pagina = 0) {
    if (this.cargando() || this.guardando()) return;
    if (!Number.isSafeInteger(this.id) || this.id <= 0) { this.error.set('Negocio no válido.'); return; }
    this.cargando.set(true);
    this.error.set('');
    forkJoin({
      permiso: this.cola.configuracion(this.id),
      negocio: this.negocios.buscar(this.id),
      servicios: this.servicio.listar(this.id, pagina)
    }).subscribe({
      next: datos => {
        this.negocio.set(datos.negocio);
        this.resultado.set(datos.servicios);
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => { this.error.set(this.mensajeError(error)); this.cargando.set(false); }
    });
  }

  editar(servicio: Servicio) {
    if (this.guardando()) return;
    this.editarId.set(servicio.id);
    this.datos = { nombre: servicio.nombre, precio: servicio.precio,
      duracionMinutos: servicio.duracionMinutos, diasRecordatorio: servicio.diasRecordatorio };
    this.error.set('');
    this.mensaje.set('');
  }

  nuevo() {
    if (this.guardando()) return;
    this.editarId.set(null);
    this.datos = this.vacio();
  }

  guardar() {
    if (this.guardando() || this.cargando()) return;
    const datos = { ...this.datos, nombre: this.datos.nombre.trim() };
    if (!datos.nombre || !Number.isInteger(datos.duracionMinutos) ||
        (datos.diasRecordatorio !== null && !Number.isInteger(datos.diasRecordatorio))) {
      this.error.set('Revisa el nombre, la duración y los días. Los minutos y días deben ser enteros.'); return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.mensaje.set('');
    const id = this.editarId();
    const peticion = id === null ? this.servicio.crear(this.id, datos) : this.servicio.actualizar(this.id, id, datos);
    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.nuevo();
        this.mensaje.set('Servicio guardado.');
        this.cargar(this.resultado()?.pagina ?? 0);
      },
      error: (error: HttpErrorResponse) => { this.guardando.set(false); this.error.set(this.mensajeError(error)); }
    });
  }

  desactivar(servicio: Servicio) {
    if (this.guardando() || this.cargando()) return;
    this.guardando.set(true);
    this.error.set('');
    this.mensaje.set('');
    this.servicio.desactivar(this.id, servicio.id).subscribe({
      next: () => {
        this.guardando.set(false);
        this.confirmarId.set(null);
        if (this.editarId() === servicio.id) this.nuevo();
        this.mensaje.set('Servicio desactivado. No se eliminó su historial.');
        const datos = this.resultado();
        this.cargar(datos && datos.contenido.length === 1 ? Math.max(0, datos.pagina - 1) : datos?.pagina ?? 0);
      },
      error: (error: HttpErrorResponse) => { this.guardando.set(false); this.error.set(this.mensajeError(error)); }
    });
  }

  private vacio(): ServicioRequest {
    return { nombre:'', precio:0, duracionMinutos:30, diasRecordatorio:null };
  }
  private mensajeError(error: HttpErrorResponse): string {
    if (error.status === 403) return 'No tienes permiso para gestionar este negocio.';
    if (error.status === 401) return 'Tu sesión venció. Inicia sesión nuevamente.';
    const datos = error.error?.data;
    if (datos && typeof datos === 'object') {
      const textos = Object.values(datos).filter((valor): valor is string => typeof valor === 'string');
      if (textos.length) return textos.join(' ');
    }
    return 'No pudimos completar la operación. Revisa los datos e intenta nuevamente.';
  }
}
