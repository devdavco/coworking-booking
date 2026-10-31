import { Espacio } from '../api/modelos';
import { Fila, clicEnBloque, construirFila, inicioBloque, rangoReserva } from './cuadricula';

const sala: Espacio = {
  id: 1,
  nombre: 'Sala Andes',
  tipo: 'SALA_REUNIONES',
  capacidad: 8,
  minutosLimpieza: 15,
  activo: true,
};
const otra: Espacio = { ...sala, id: 2, nombre: 'Cabina 1' };
const FECHA = '2026-11-03';
const DIA_ANTERIOR = new Date('2026-11-02T12:00:00Z');

function franja(desde: string, hasta: string) {
  return { desde: `${desde}-05:00`, hasta: `${hasta}-05:00` };
}

function indicesCon(fila: Fila, estado: string): number[] {
  return fila.estados.flatMap((e, i) => (e === estado ? [i] : []));
}

describe('construirFila', () => {
  it('los bloques van de 07:00 a 21:45', () => {
    expect(inicioBloque(0)).toBe(420);
    expect(inicioBloque(59)).toBe(1305);
  });

  it('sin franjas todo está libre', () => {
    const fila = construirFila(sala, [], FECHA, DIA_ANTERIOR);
    expect(fila.estados).toHaveLength(60);
    expect(fila.estados.every((e) => e === 'libre')).toBe(true);
  });

  it('marca como ocupados los bloques de una franja con su limpieza', () => {
    const fila = construirFila(sala, [franja('2026-11-03T09:00:00', '2026-11-03T10:15:00')], FECHA, DIA_ANTERIOR);
    expect(indicesCon(fila, 'ocupado')).toEqual([8, 9, 10, 11, 12]);
    expect(fila.estados[7]).toBe('libre');
    expect(fila.estados[13]).toBe('libre');
  });

  it('una franja que termina después de las 22:00 marca los últimos bloques sin salirse', () => {
    const fila = construirFila(sala, [franja('2026-11-03T21:30:00', '2026-11-03T22:15:00')], FECHA, DIA_ANTERIOR);
    expect(fila.estados).toHaveLength(60);
    expect(indicesCon(fila, 'ocupado')).toEqual([58, 59]);
  });

  it('una franja de otro día no marca nada', () => {
    const fila = construirFila(sala, [franja('2026-11-04T09:00:00', '2026-11-04T10:00:00')], FECHA, DIA_ANTERIOR);
    expect(indicesCon(fila, 'ocupado')).toEqual([]);
  });

  it('hoy a las 10:07 los bloques hasta el de 10:00 ya pasaron', () => {
    const fila = construirFila(sala, [], FECHA, new Date('2026-11-03T15:07:00Z'));
    expect(indicesCon(fila, 'pasado')).toEqual([0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]);
    expect(fila.estados[13]).toBe('libre');
  });
});

describe('clicEnBloque', () => {
  const fila = construirFila(sala, [franja('2026-11-03T13:00:00', '2026-11-03T14:00:00')], FECHA, DIA_ANTERIOR);
  const filaOtra = construirFila(otra, [], FECHA, DIA_ANTERIOR);

  it('el primer clic marca el inicio', () => {
    expect(clicEnBloque(null, fila, 16)).toEqual({ espacioId: 1, desde: 16, hasta: 16 });
  });

  it('el segundo clic en la misma fila marca el final', () => {
    expect(clicEnBloque({ espacioId: 1, desde: 16, hasta: 16 }, fila, 21)).toEqual({ espacioId: 1, desde: 16, hasta: 21 });
  });

  it('un clic antes del inicio vuelve a empezar desde ese bloque', () => {
    expect(clicEnBloque({ espacioId: 1, desde: 16, hasta: 16 }, fila, 10)).toEqual({ espacioId: 1, desde: 10, hasta: 10 });
  });

  it('un clic en otra fila vuelve a empezar en esa fila', () => {
    expect(clicEnBloque({ espacioId: 1, desde: 16, hasta: 16 }, filaOtra, 20)).toEqual({ espacioId: 2, desde: 20, hasta: 20 });
  });

  it('un rango que atraviesa un bloque ocupado vuelve a empezar desde el bloque clicado', () => {
    // Bloques 24 a 27 (13:00 a 14:00) están ocupados.
    expect(clicEnBloque({ espacioId: 1, desde: 20, hasta: 20 }, fila, 30)).toEqual({ espacioId: 1, desde: 30, hasta: 30 });
  });

  it('un clic en un bloque ocupado no selecciona nada', () => {
    expect(clicEnBloque(null, fila, 25)).toBeNull();
  });

  it('un clic en un bloque pasado no selecciona nada', () => {
    const hoy = construirFila(sala, [], FECHA, new Date('2026-11-03T15:07:00Z'));
    expect(clicEnBloque(null, hoy, 5)).toBeNull();
  });
});

describe('rangoReserva', () => {
  it('convierte la selección en horas ISO de Bogotá', () => {
    expect(rangoReserva({ espacioId: 1, desde: 16, hasta: 21 }, FECHA)).toEqual({
      horaInicio: '2026-11-03T11:00:00-05:00',
      horaFin: '2026-11-03T12:30:00-05:00',
    });
  });
});
