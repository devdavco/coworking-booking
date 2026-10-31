import { HttpErrorResponse } from '@angular/common/http';
import { Problema } from './modelos';

/** Texto para mostrar al usuario a partir de cualquier error de una petición. */
export function mensajeDeError(err: unknown): string {
  if (!(err instanceof HttpErrorResponse)) {
    return 'Ocurrió un error inesperado';
  }
  if (err.status === 0) {
    return 'No se pudo conectar con el servidor';
  }
  const detalle = comoProblema(err.error)?.detail;
  return detalle ?? `Ocurrió un error inesperado (${err.status})`;
}

/** Errores de validación por campo (la propiedad "errores" de un 400), o {} si no hay. */
export function erroresPorCampo(err: unknown): Record<string, string> {
  if (!(err instanceof HttpErrorResponse)) {
    return {};
  }
  return comoProblema(err.error)?.errores ?? {};
}

function comoProblema(cuerpo: unknown): Problema | null {
  return typeof cuerpo === 'object' && cuerpo !== null ? (cuerpo as Problema) : null;
}
