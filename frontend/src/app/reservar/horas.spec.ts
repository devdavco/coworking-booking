import { enBogota, formatoFecha, formatoHora, hoyEnBogota, isoBogota, sumarDias } from './horas';

describe('enBogota', () => {
  it('lee una hora que ya viene en -05:00', () => {
    expect(enBogota('2026-11-03T09:00:00-05:00')).toEqual({ fecha: '2026-11-03', minutos: 540 });
  });

  it('convierte una hora en UTC a hora de Bogotá', () => {
    expect(enBogota('2026-11-03T14:00:00Z')).toEqual({ fecha: '2026-11-03', minutos: 540 });
  });

  it('convierte una hora de otra zona a hora de Bogotá', () => {
    expect(enBogota('2026-11-03T15:00:00+01:00')).toEqual({ fecha: '2026-11-03', minutos: 540 });
  });

  it('una hora UTC de madrugada cae en el día anterior en Bogotá', () => {
    expect(enBogota('2026-11-04T03:30:00Z')).toEqual({ fecha: '2026-11-03', minutos: 1350 });
  });
});

describe('fechas y formato', () => {
  it('hoy en Bogotá puede ser el día anterior al de UTC', () => {
    expect(hoyEnBogota(new Date('2026-11-03T04:00:00Z'))).toBe('2026-11-02');
  });

  it('suma días cruzando el año', () => {
    expect(sumarDias('2026-12-31', 1)).toBe('2027-01-01');
  });

  it('arma una fecha ISO en -05:00', () => {
    expect(isoBogota('2026-11-03', 690)).toBe('2026-11-03T11:30:00-05:00');
  });

  it('formatea minutos como hora', () => {
    expect(formatoHora(1320)).toBe('22:00');
  });

  it('formatea una fecha como dd/mm/aaaa', () => {
    expect(formatoFecha('2026-11-03')).toBe('03/11/2026');
  });
});
