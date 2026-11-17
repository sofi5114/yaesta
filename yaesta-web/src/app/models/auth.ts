export interface Usuario {
  id: number;
  nombre: string;
  correo: string;
  rol: 'CLIENTE' | 'NEGOCIO';
  aceptaRecordatorios: boolean;
}

export interface LoginResponse {
  token: string;
  usuario: Usuario;
}

export interface RegistroRequest {
  nombre: string;
  correo: string;
  clave: string;
  aceptaRecordatorios: boolean;
}