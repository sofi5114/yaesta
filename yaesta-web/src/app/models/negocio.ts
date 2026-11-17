export interface Negocio {
  id: number;
  nombre: string;
  categoria: string;
  distrito: string;
}

export interface Respuesta<T> {
  status: string;
  data: T;
}