import { Espacio, FranjaOcupada } from '../api/modelos';
import { APERTURA_MINUTOS, BLOQUE_MINUTOS, CIERRE_MINUTOS } from '../config';
import { hoyEnBogota, isoBogota, minutosDesdeElInicioDe } from './horas';

export type EstadoBloque = 'libre' | 'ocupado' | 'pasado';

/** Una fila de la cuadrícula: un espacio y el estado de cada bloque de 15 minutos del día. */
export interface Fila {
  espacio: Espacio;
  estados: EstadoBloque[];
}

/** Bloques seleccionados de un espacio, con índices inclusivos. */
export interface Seleccion {
  espacioId: number;
  desde: number;
  hasta: number;
}

export const TOTAL_BLOQUES = (CIERRE_MINUTOS - APERTURA_MINUTOS) / BLOQUE_MINUTOS;

/** Minutos desde medianoche en que empieza el bloque. */
export function inicioBloque(indice: number): number {
  return APERTURA_MINUTOS + indice * BLOQUE_MINUTOS;
}

export function construirFila(espacio: Espacio, franjas: FranjaOcupada[], fecha: string, ahora: Date): Fila {
  const intervalos = franjas.map((franja) => ({
    desde: minutosDesdeElInicioDe(fecha, franja.desde),
    hasta: minutosDesdeElInicioDe(fecha, franja.hasta),
  }));
  const hoy = hoyEnBogota(ahora);
  const minutosAhora = minutosDesdeElInicioDe(fecha, ahora.toISOString());

  const estados = Array.from({ length: TOTAL_BLOQUES }, (_, indice): EstadoBloque => {
    const inicio = inicioBloque(indice);
    const fin = inicio + BLOQUE_MINUTOS;
    if (fecha < hoy || (fecha === hoy && inicio < minutosAhora)) {
      return 'pasado';
    }
    const ocupado = intervalos.some((intervalo) => intervalo.desde < fin && intervalo.hasta > inicio);
    return ocupado ? 'ocupado' : 'libre';
  });
  return { espacio, estados };
}

/**
 * Primer clic: marca el inicio. Segundo clic en la misma fila, igual o posterior al inicio y
 * sin bloques no libres entre medio: marca el final. Cualquier otro clic en un bloque libre
 * empieza de nuevo desde ahí. Un clic en un bloque que no está libre borra la selección.
 */
export function clicEnBloque(actual: Seleccion | null, fila: Fila, indice: number): Seleccion | null {
  if (fila.estados[indice] !== 'libre') {
    return null;
  }
  const nueva: Seleccion = { espacioId: fila.espacio.id, desde: indice, hasta: indice };
  const esperandoFinal = actual !== null && actual.espacioId === fila.espacio.id && actual.desde === actual.hasta;
  if (!esperandoFinal || indice < actual.desde) {
    return nueva;
  }
  const todosLibres = fila.estados.slice(actual.desde, indice + 1).every((estado) => estado === 'libre');
  return todosLibres ? { ...actual, hasta: indice } : nueva;
}

export function rangoReserva(seleccion: Seleccion, fecha: string): { horaInicio: string; horaFin: string } {
  return {
    horaInicio: isoBogota(fecha, inicioBloque(seleccion.desde)),
    horaFin: isoBogota(fecha, inicioBloque(seleccion.hasta) + BLOQUE_MINUTOS),
  };
}
