export const CATEGORIAS = [
  {
    nombre: 'Restaurantes',
    foto: 0,
    claves: ['restaurante', 'restobar', 'cevicher', 'pizzeria', 'comida'],
  },
  { nombre: 'Cafeterías', foto: 1, claves: ['cafe', 'cafeter', 'pasteler'] },
  { nombre: 'Barberías', foto: 2, claves: ['barber', 'peluquer', 'corte'] },
  {
    nombre: 'Belleza',
    foto: 3,
    claves: ['belleza', 'una', 'manicure', 'estetica', 'salon', 'maquillaje', 'spa'],
  },
  { nombre: 'Mascotas', foto: 4, claves: ['veterin', 'mascota', 'grooming', 'pet'] },
  { nombre: 'Talleres', foto: 5, claves: ['taller', 'reparacion', 'biciclet', 'mecanica'] },
  { nombre: 'Lavanderías', foto: 6, claves: ['lavander', 'tintorer'] },
  { nombre: 'Servicios locales', foto: 7, claves: [] },
];
export function fotoCategoria(categoria: string): number {
  const texto = categoria
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
  return CATEGORIAS.find((c) => c.claves.some((k) => texto.includes(k)))?.foto ?? 7;
}
