export interface Recordatorio {
  id: number;
  turnoId: number;
  negocioId: number;
  negocio: string;
  servicioId: number;
  servicio: string;
  fechaProgramada: string;
  estado:
    | 'PENDIENTE'
    | 'PROCESANDO'
    | 'ENVIADO'
    | 'CANCELADO'
    | 'ERROR';
  enviadoEn: string | null;
}