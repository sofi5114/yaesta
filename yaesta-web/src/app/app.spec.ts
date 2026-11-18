import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { of } from 'rxjs';
const locales=[{id:1,nombre:'Huella Sana',categoria:'Veterinaria',distrito:'Trujillo'},{id:2,nombre:'Estudio 28',categoria:'Barbería',distrito:'Víctor Larco Herrera'},{id:3,nombre:'Brillo Studio',categoria:'Salón de uñas',distrito:'Trujillo'}];
import { App } from './app';
import { AuthService } from './services/auth.service';
import { NegocioService } from './services/negocio.service';

describe('Navegación de YAESTÁ', () => {
  const usuario = signal<any>(null);
  beforeEach(async () => {
    usuario.set(null);
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { usuario, cerrarSesion: () => usuario.set(null) } },
        { provide: NegocioService, useValue: { listar: () => of(locales), misNegocios: () => of(locales) } },
      ],
    }).compileComponents();
  });
  it('mantiene el catálogo disponible sin membresía ni sesión', async () => {
    const f = TestBed.createComponent(App);
    await f.whenStable();
    expect(f.nativeElement.querySelector('h1')?.textContent).toContain('Tu tiempo vale.');
    expect(f.nativeElement.querySelector('a[href="/login"]')).not.toBeNull();
    expect(f.nativeElement.querySelectorAll('.local').length).toBe(3);
  });
  it('permite acceder a Plus desde una cuenta de cliente', async () => {
    usuario.set({ id: 1, nombre: 'Ana', rol: 'CLIENTE' });
    const f = TestBed.createComponent(App);
    await f.whenStable();
    expect(f.nativeElement.querySelector('a[href="/plus"]')?.textContent).toContain('YAESTÁ Plus');
    expect(f.nativeElement.querySelector('a[href="/turnos"]')).not.toBeNull();
  });
  it('conserva el panel del negocio al incorporar Plus', async () => {
    usuario.set({ id: 2, nombre: 'Local', rol: 'NEGOCIO' });
    const f = TestBed.createComponent(App);
    await f.whenStable();
    expect(f.nativeElement.querySelector('h1')?.textContent).toContain('Gestiona tu negocio');
    expect(f.nativeElement.querySelector('a[href="/plus"]')).not.toBeNull();
  });
  it('encuentra salones de uñas y veterinarias en las categorías visuales correspondientes',async()=>{
    const f=TestBed.createComponent(App);await f.whenStable();
    f.componentInstance.categoria.set('Belleza');f.detectChanges();
    expect(f.nativeElement.querySelectorAll('.local').length).toBe(1);
    expect(f.nativeElement.querySelector('.local h3').textContent).toContain('Brillo Studio');
    f.componentInstance.categoria.set('Mascotas');f.detectChanges();
    expect(f.nativeElement.querySelector('.local h3').textContent).toContain('Huella Sana');
  });
  it('combina búsqueda y distrito sin inventar resultados para nuevas categorías',async()=>{
    const f=TestBed.createComponent(App);await f.whenStable();
    f.componentInstance.distrito.set('Trujillo');f.componentInstance.busqueda.set('Estudio');f.detectChanges();
    expect(f.nativeElement.querySelectorAll('.local').length).toBe(0);
    expect(f.nativeElement.textContent).toContain('Aún no hay locales');
    f.componentInstance.limpiar();f.componentInstance.categoria.set('Restaurantes');f.detectChanges();
    expect(f.nativeElement.querySelectorAll('.local').length).toBe(0);
    f.componentInstance.limpiar();f.detectChanges();
    expect(f.nativeElement.querySelectorAll('.local').length).toBe(3);
  });
  it('enlaza cada local con su ficha real y usa sastrería para servicios locales',async()=>{
    const f=TestBed.createComponent(App);await f.whenStable();
    expect(f.nativeElement.querySelector('.local a').getAttribute('href')).toBe('/negocios/1');
    const botones=[...f.nativeElement.querySelectorAll('.categoria')] as HTMLElement[];
    expect(botones.find(b=>b.textContent?.includes('Servicios locales'))?.querySelector('.f-7')).not.toBeNull();
  });

});
