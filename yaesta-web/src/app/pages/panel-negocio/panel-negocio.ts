import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin, Observable, timer } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AuthService } from '../../services/auth.service';
import {
    AccionCola,
    ColaService
} from '../../services/cola.service';
import { NegocioService } from '../../services/negocio.service';

import { Negocio } from '../../models/negocio';
import { Pagina, Servicio } from '../../models/servicio';
import { Turno } from '../../models/turno';
import { ServicioService } from '../../services/servicio.service';

@Component({
    selector: 'app-panel-negocio',
    imports: [FormsModule, RouterLink],
    templateUrl: './panel-negocio.html',
    styleUrl: './panel-negocio.css'
})
export class PanelNegocio {
    readonly auth = inject(AuthService);

    private readonly ruta = inject(ActivatedRoute);
    private readonly cola = inject(ColaService);
    private readonly negocios = inject(NegocioService);

    private readonly negocioId = Number(
        this.ruta.snapshot.paramMap.get('id')
    );

    private readonly servicios = inject(ServicioService);

    readonly serviciosDisponibles = signal<Pagina<Servicio> | null>(null);
    readonly cargandoServicios = signal(false);

    nombrePresencial = '';
    servicioPresencialId: number | null = null;

    readonly negocio = signal<Negocio | null>(null);
    readonly resultado = signal<Pagina<Turno> | null>(null);
    readonly cargando = signal(false);
    readonly procesando = signal(false);
    readonly error = signal('');
    readonly mensaje = signal('');

    private actualizandoCola = false;
    aceptaTurnos = false;
    toleranciaMinutos = 5;

    readonly estados: Record<Turno['estado'], string> = {
        EN_ESPERA: 'En espera',
        LLAMADO: 'Llamado',
        ATENDIENDO: 'En atención',
        ATENDIDO: 'Atendido',
        CANCELADO: 'Cancelado',
        AUSENTE: 'Ausente'
    };

    constructor() {
        timer(15000, 15000).pipe(takeUntilDestroyed()).subscribe(() => {
            if (this.negocio() && !this.cargando() && !this.procesando() &&
                !this.cargandoServicios() && !this.actualizandoCola &&
                this.auth.usuario()?.rol === 'NEGOCIO' && document.visibilityState === 'visible') {
                this.actualizandoCola = true;
                this.cola.listar(this.negocioId, this.resultado()?.pagina ?? 0).subscribe({
                    next: datos => {
                        if (!this.cargando() && !this.procesando()) this.resultado.set(datos);
                        this.actualizandoCola = false;
                    },
                    error: () => { this.actualizandoCola = false; }
                });
            }
        });
        if (this.auth.usuario()?.rol === 'NEGOCIO') {
            this.cargar();
        }
    }

    cargar(pagina = this.resultado()?.pagina ?? 0) {
        if (this.cargando() || this.procesando()) {
            return;
        }

        if (!Number.isSafeInteger(this.negocioId) || this.negocioId <= 0) {
            this.error.set('El negocio no es válido.');
            return;
        }

        this.cargando.set(true);
        this.error.set('');

        forkJoin({
            negocio: this.negocios.buscar(this.negocioId),
            configuracion: this.cola.configuracion(this.negocioId),
            turnos: this.cola.listar(this.negocioId, pagina),
            servicios: this.servicios.listar(this.negocioId)
        }).subscribe({
            next: datos => {
                this.negocio.set(datos.negocio);
                this.resultado.set(datos.turnos);
                this.serviciosDisponibles.set(datos.servicios);
                this.aceptaTurnos = datos.configuracion.aceptaTurnos;
                this.toleranciaMinutos = datos.configuracion.toleranciaMinutos;
                this.cargando.set(false);
            },
            error: (respuesta: HttpErrorResponse) => {
                this.error.set(this.mensajeError(respuesta));
                this.cargando.set(false);
            }
        });
    }
    guardarConfiguracion() {
        if (
            !Number.isInteger(this.toleranciaMinutos) ||
            this.toleranciaMinutos < 1 ||
            this.toleranciaMinutos > 30
        ) {
            this.error.set('La tolerancia debe ser de 1 a 30 minutos.');
            return;
        }

        this.ejecutar(
            this.cola.configurar(
                this.negocioId,
                this.aceptaTurnos,
                this.toleranciaMinutos
            ),
            'Configuración guardada.'
        );
    }

    llamarSiguiente() {
        this.ejecutar(
            this.cola.llamarSiguiente(this.negocioId),
            'Se llamó al siguiente cliente.'
        );
    }

    gestionar(turno: Turno, accion: AccionCola) {
        this.ejecutar(
            this.cola.gestionar(this.negocioId, turno.id, accion),
            'Turno actualizado.'
        );
    }

    private ejecutar(
        operacion: Observable<unknown>,
        mensaje: string
    ) {
        if (this.procesando() || this.cargando()) {
            return;
        }

        this.procesando.set(true);
        this.error.set('');
        this.mensaje.set('');

        operacion.subscribe({
            next: () => {
                this.procesando.set(false);
                this.mensaje.set(mensaje);
                this.cargar(0);
            },
            error: (respuesta: HttpErrorResponse) => {
                this.procesando.set(false);
                this.error.set(this.mensajeError(respuesta));
            }
        });
    }

    private mensajeError(respuesta: HttpErrorResponse): string {
        if (respuesta.status === 401) {
            return 'Tu sesión venció. Inicia sesión nuevamente.';
        }

        if (respuesta.status === 403) {
            return 'Tu cuenta no tiene permiso para gestionar este negocio.';
        }

        const mensaje = respuesta.error?.data?.mensaje;

        return typeof mensaje === 'string'
            ? mensaje
            : 'No pudimos completar la operación. Intenta nuevamente.';
    }

    cambiarPaginaServicios(pagina: number) {
        const actual = this.serviciosDisponibles();

        if (
            !actual ||
            this.cargandoServicios() ||
            this.cargando() ||
            this.procesando() ||
            pagina < 0 ||
            pagina >= actual.totalPaginas
        ) {
            return;
        }

        this.cargandoServicios.set(true);
        this.error.set('');

        this.servicios.listar(this.negocioId, pagina).subscribe({
            next: datos => {
                this.serviciosDisponibles.set(datos);
                this.cargandoServicios.set(false);
            },
            error: (respuesta: HttpErrorResponse) => {
                this.error.set(this.mensajeError(respuesta));
                this.cargandoServicios.set(false);
            }
        });
    }

    registrarPresencial() {
        const nombre = this.nombrePresencial.trim();
        const servicioId = this.servicioPresencialId;

        if (
            this.procesando() ||
            this.cargando() ||
            this.cargandoServicios()
        ) {
            return;
        }

        if (!nombre || nombre.length > 100 || servicioId === null) {
            this.error.set('Escribe el nombre del cliente y selecciona un servicio.');
            return;
        }

        this.procesando.set(true);
        this.error.set('');
        this.mensaje.set('');

        this.cola.registrarPresencial(
            this.negocioId,
            servicioId,
            nombre
        ).subscribe({
            next: turno => {
                this.nombrePresencial = '';
                this.servicioPresencialId = null;
                this.procesando.set(false);
                this.mensaje.set(
                    `Cliente registrado. Su turno es el #${turno.id}.`
                );
                this.cargar(0);
            },
            error: (respuesta: HttpErrorResponse) => {
                this.procesando.set(false);
                this.error.set(this.mensajeError(respuesta));
            }
        });
    }
}
