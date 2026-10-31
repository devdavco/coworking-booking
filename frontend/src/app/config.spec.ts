import { APERTURA_MINUTOS, BLOQUE_MINUTOS, CIERRE_MINUTOS, NOMBRE_TIPO, OFFSET_MINUTOS, TIPOS_ESPACIO } from './config';

describe('config', () => {
  it('el horario de 07:00 a 22:00 tiene 60 bloques de 15 minutos', () => {
    expect((CIERRE_MINUTOS - APERTURA_MINUTOS) / BLOQUE_MINUTOS).toBe(60);
  });

  it('Bogotá está en UTC-5', () => {
    expect(OFFSET_MINUTOS).toBe(-300);
  });

  it('cada tipo de espacio tiene un nombre para mostrar', () => {
    expect(Object.keys(NOMBRE_TIPO).sort()).toEqual([...TIPOS_ESPACIO].sort());
  });
});
