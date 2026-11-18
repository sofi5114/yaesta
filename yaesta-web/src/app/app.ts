import { CATEGORIAS } from './shared/categorias-visuales';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Negocio } from './models/negocio';
import { NegocioService } from './services/negocio.service';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly servicio = inject(NegocioService);
  readonly auth = inject(AuthService);
  readonly router = inject(Router);
  readonly negocios = signal<Negocio[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly busqueda = signal('');
  readonly distrito = signal('');
  readonly categoria = signal('Todos');
  readonly orden = signal<'recomendados' | 'nombre'>('recomendados');
  readonly categorias = CATEGORIAS;
  readonly distritos = computed(() =>
    [...new Set(this.negocios().map((n) => n.distrito))].sort((a, b) => a.localeCompare(b, 'es')),
  );
  readonly filtrados = computed(() => {
    const texto = this.normalizar(this.busqueda());
    const lista = this.negocios().filter(
      (n) =>
        (!this.distrito() || n.distrito === this.distrito()) &&
        (this.categoria() === 'Todos' || this.categoriaDe(n).nombre === this.categoria()) &&
        this.normalizar(`${n.nombre} ${n.categoria} ${n.distrito}`).includes(texto),
    );
    return this.orden() === 'nombre'
      ? [...lista].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))
      : lista;
  });
  constructor() {
    effect((onCleanup) => {
      // Only session changes reload the catalogue; filter signals remain independent.
      this.auth.usuario();
      const consulta = this.cargar();
      onCleanup(() => consulta.unsubscribe());
    });
  }
  cargar() {
    const propietario = this.auth.usuario()?.rol === 'NEGOCIO';
    this.cargando.set(true);
    this.error.set('');
    return (propietario ? this.servicio.misNegocios() : this.servicio.listar()).subscribe({
      next: (n) => {
        this.negocios.set(n);
        this.cargando.set(false);
      },
      error: () => {
        this.negocios.set([]);
        this.error.set('No pudimos cargar los negocios. Revisa tu conexión e intenta nuevamente.');
        this.cargando.set(false);
      },
    });
  }
  categoriaDe(n: Negocio) {
    const texto = this.normalizar(n.categoria);
    return CATEGORIAS.find((c) => c.claves.some((k) => texto.includes(k))) ?? CATEGORIAS[7];
  }
  fotoClase(n: Negocio) {
    return 'f-' + this.categoriaDe(n).foto;
  }
  elegirCategoria(nombre: string) {
    this.categoria.set(this.categoria() === nombre ? 'Todos' : nombre);
    this.irAlCatalogo();
  }
  limpiar() {
    this.busqueda.set('');
    this.distrito.set('');
    this.categoria.set('Todos');
    this.orden.set('recomendados');
  }
  buscar(event: Event) {
    event.preventDefault();
    this.irAlCatalogo();
  }
  irAlCatalogo() {
    document
      .getElementById('catalogo')
      ?.scrollIntoView({
        behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches
          ? 'instant'
          : 'smooth',
        block: 'start',
      });
  }
  iniciales(nombre: string) {
    return nombre
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((x) => x[0])
      .join('');
  }
  private normalizar(s: string) {
    return s
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
  }
  cerrarSesion() {
    this.auth.cerrarSesion();
    void this.router.navigateByUrl('/');
  }
}
