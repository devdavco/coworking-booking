import { OFFSET_MINUTOS, ZONA_OFFSET } from '../config';

/*
 * Todo se calcula en hora de Bogotá con aritmética UTC, sin usar la zona del navegador:
 * así la app muestra lo mismo aunque el computador esté en Madrid o en UTC.
 */

const MS_POR_MINUTO = 60_000;

/** Fecha (YYYY-MM-DD) y minutos desde medianoche, en Bogotá, de un instante ISO con cualquier offset. */
export function enBogota(iso: string): { fecha: string; minutos: number } {
  const local = new Date(Date.parse(iso) + OFFSET_MINUTOS * MS_POR_MINUTO);
  return {
    fecha: local.toISOString().slice(0, 10),
    minutos: local.getUTCHours() * 60 + local.getUTCMinutes(),
  };
}

/** Minutos entre la medianoche de "fecha" en Bogotá y el instante ISO (puede ser negativo o pasar de 1440). */
export function minutosDesdeElInicioDe(fecha: string, iso: string): number {
  return (Date.parse(iso) - Date.parse(`${fecha}T00:00:00${ZONA_OFFSET}`)) / MS_POR_MINUTO;
}

export function hoyEnBogota(ahora: Date): string {
  return enBogota(ahora.toISOString()).fecha;
}

export function sumarDias(fecha: string, dias: number): string {
  const dia = new Date(`${fecha}T00:00:00Z`);
  dia.setUTCDate(dia.getUTCDate() + dias);
  return dia.toISOString().slice(0, 10);
}

/** "2026-11-03" y 690 → "2026-11-03T11:30:00-05:00". */
export function isoBogota(fecha: string, minutos: number): string {
  return `${fecha}T${formatoHora(minutos)}:00${ZONA_OFFSET}`;
}

/** 690 → "11:30". */
export function formatoHora(minutos: number): string {
  const dosDigitos = (n: number) => String(n).padStart(2, '0');
  return `${dosDigitos(Math.floor(minutos / 60))}:${dosDigitos(minutos % 60)}`;
}

/** "2026-11-03" → "03/11/2026". */
export function formatoFecha(fecha: string): string {
  const [anio, mes, dia] = fecha.split('-');
  return `${dia}/${mes}/${anio}`;
}
