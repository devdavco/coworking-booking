/**
 * Constantes del coworking. Deben coincidir con app.reservas del backend:
 * si allá cambian el horario o la anticipación, se cambian aquí también.
 */

/** Bogotá no tiene horario de verano: siempre UTC-5. */
export const ZONA_OFFSET = '-05:00';
export const OFFSET_MINUTOS = -300;

/** 07:00 y 22:00, en minutos desde medianoche. */
export const APERTURA_MINUTOS = 7 * 60;
export const CIERRE_MINUTOS = 22 * 60;
export const BLOQUE_MINUTOS = 15;

export const ANTICIPACION_DIAS = 90;

export const TIPOS_ESPACIO = ['ESCRITORIO', 'SALA_REUNIONES', 'SALA_CONFERENCIAS', 'CABINA'] as const;
export type TipoEspacio = (typeof TIPOS_ESPACIO)[number];

export const NOMBRE_TIPO: Record<TipoEspacio, string> = {
  ESCRITORIO: 'Escritorio',
  SALA_REUNIONES: 'Sala de reuniones',
  SALA_CONFERENCIAS: 'Sala de conferencias',
  CABINA: 'Cabina',
};
