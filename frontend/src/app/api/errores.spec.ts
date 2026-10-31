import { HttpErrorResponse } from '@angular/common/http';
import { erroresPorCampo, mensajeDeError } from './errores';

describe('mensajeDeError', () => {
  it('sin conexión con el servidor', () => {
    expect(mensajeDeError(new HttpErrorResponse({ status: 0 }))).toBe('No se pudo conectar con el servidor');
  });

  it('usa el detail del ProblemDetail tal cual', () => {
    const err = new HttpErrorResponse({
      status: 409,
      error: { detail: 'El espacio ya está ocupado de 09:00 a 10:15 (incluye limpieza)' },
    });
    expect(mensajeDeError(err)).toBe('El espacio ya está ocupado de 09:00 a 10:15 (incluye limpieza)');
  });

  it('una respuesta que no es ProblemDetail da un mensaje genérico con el código', () => {
    const err = new HttpErrorResponse({ status: 500, error: '<html>Bad gateway</html>' });
    expect(mensajeDeError(err)).toBe('Ocurrió un error inesperado (500)');
  });

  it('un error que no es HTTP da un mensaje genérico', () => {
    expect(mensajeDeError(new Error('boom'))).toBe('Ocurrió un error inesperado');
  });
});

describe('erroresPorCampo', () => {
  it('devuelve los errores por campo de un 400', () => {
    const err = new HttpErrorResponse({
      status: 400,
      error: {
        detail: 'La petición tiene campos inválidos',
        errores: { password: 'no puede ocupar más de 72 bytes' },
      },
    });
    expect(erroresPorCampo(err)).toEqual({ password: 'no puede ocupar más de 72 bytes' });
  });

  it('devuelve un objeto vacío si no hay errores por campo', () => {
    expect(erroresPorCampo(new HttpErrorResponse({ status: 409, error: { detail: 'x' } }))).toEqual({});
  });
});
