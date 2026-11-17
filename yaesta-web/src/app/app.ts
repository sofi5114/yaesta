import {Component,computed,effect,inject,signal} from '@angular/core';
import { Negocio } from './models/negocio';
import { NegocioService } from './services/negocio.service';
import { AuthService } from './services/auth.service';
import {Router,RouterLink,RouterLinkActive,RouterOutlet} from '@angular/router';



@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'

})
export class App {
  private readonly servicio = inject(NegocioService);

  readonly negocios = signal<Negocio[]>([]);
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly busqueda = signal('');
  readonly categoria = signal('Todos');

  readonly categorias = ['Todos', 'Uñas', 'Barbería', 'Veterinaria'];
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly filtrados = computed(() => {
    const texto = this.normalizar(this.busqueda());
    const categoria = this.normalizar(this.categoria());

    return this.negocios().filter(negocio => {
      const contenido = this.normalizar(
        `${negocio.nombre} ${negocio.categoria} ${negocio.distrito}`
      );

      const coincideCategoria =
        categoria === 'todos' ||
        this.normalizar(negocio.categoria).includes(categoria);

      return contenido.includes(texto) && coincideCategoria;
    });
  });

 constructor() {
  effect(onCleanup => {
    this.busqueda.set('');
    this.categoria.set('Todos');

    const consulta = this.cargar();

    onCleanup(() => consulta.unsubscribe());
  });
}
 cargar() {
  const esPropietario = this.auth.usuario()?.rol === 'NEGOCIO';

  this.cargando.set(true);
  this.error.set('');
  this.negocios.set([]);

  const consulta = esPropietario
    ? this.servicio.misNegocios()
    : this.servicio.listar();

  return consulta.subscribe({
    next: negocios => {
      this.negocios.set(negocios);
      this.cargando.set(false);
    },
    error: () => {
      this.error.set(
        'No pudimos cargar los negocios. Intenta nuevamente.'
      );
      this.cargando.set(false);
    }
  });
}

  iniciales(nombre: string): string {
    return nombre
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map(palabra => palabra[0])
      .join('');
  }

  private normalizar(texto: string): string {
    return texto
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