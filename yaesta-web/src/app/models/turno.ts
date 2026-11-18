export interface Turno {
    id: number;
    negocioId: number;
    negocio: string;
    servicioId: number;
    servicio: string;
    nombreCliente: string;
    estado:
    | 'EN_ESPERA'
    | 'LLAMADO'
    | 'ATENDIENDO'
    | 'ATENDIDO'
    | 'CANCELADO'
    | 'AUSENTE';
    posicion: number | null;
    esperaEstimadaMinutos: number | null;

    llegadaEn: string | null;
    llamadoEn: string | null;
    toleranciaMinutos: number;
    finEstimado: string | null;
    noLlamarAntesDe: string | null;
    demoraSignificativa: boolean;
    esperaInicialMinutos: number | null;
}