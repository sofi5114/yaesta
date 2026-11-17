export interface Servicio {
  id: number;
  negocioId: number;
  nombre: string;
  precio: number;
  duracionMinutos: number;
  diasRecordatorio: number | null;
  activo: boolean;
}

export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}