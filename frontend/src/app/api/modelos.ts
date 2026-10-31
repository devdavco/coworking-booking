import { TipoEspacio } from '../config';

export type { TipoEspacio };

export interface Espacio {
  id: number;
  nombre: string;
  tipo: TipoEspacio;
  capacidad: number;
  minutosLimpieza: number;
  activo: boolean;
}

export interface EspacioRequest {
  nombre: string;
  tipo: TipoEspacio;
  capacidad: number;
  minutosLimpieza: number;
}

/** Intervalo [desde, hasta) ocupado, con la limpieza incluida. */
export interface FranjaOcupada {
  desde: string;
  hasta: string;
}

export type EstadoReserva = 'CONFIRMADA' | 'CANCELADA';

export interface Reserva {
  id: number;
  usuarioId: number;
  espacioId: number;
  espacioNombre: string;
  horaInicio: string;
  horaFin: string;
  horaFinTotal: string;
  estado: EstadoReserva;
  finalizada: boolean;
}

export interface CrearReservaRequest {
  espacioId: number;
  horaInicio: string;
  horaFin: string;
}

export interface Usuario {
  id: number;
  nombre: string;
  email: string;
  rol: 'ADMIN' | 'USUARIO';
}

export interface TokenResponse {
  token: string;
  expiraEn: string;
}

/** Cuerpo de error ProblemDetail (RFC 7807) que devuelve el backend. */
export interface Problema {
  title?: string;
  detail?: string;
  status?: number;
  errores?: Record<string, string>;
}
