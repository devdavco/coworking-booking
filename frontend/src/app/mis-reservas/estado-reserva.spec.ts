import { Reserva } from '../api/modelos';
import { puedeCancelar, textoEstado } from './estado-reserva';

function reserva(cambios: Partial<Reserva>): Reserva {
  return {
    id: 1,
    usuarioId: 7,
    espacioId: 1,
    espacioNombre: 'Sala Andes',
    horaInicio: '2026-11-03T09:00:00-05:00',
    horaFin: '2026-11-03T10:00:00-05:00',
    horaFinTotal: '2026-11-03T10:15:00-05:00',
    estado: 'CONFIRMADA',
    finalizada: false,
    ...cambios,
  };
}

describe('textoEstado', () => {
  it('una cancelada es Cancelada aunque ya haya pasado', () => {
    expect(textoEstado(reserva({ estado: 'CANCELADA', finalizada: true }))).toBe('Cancelada');
  });

  it('una confirmada que ya terminó es Finalizada', () => {
    expect(textoEstado(reserva({ finalizada: true }))).toBe('Finalizada');
  });

  it('una confirmada pendiente es Confirmada', () => {
    expect(textoEstado(reserva({}))).toBe('Confirmada');
  });
});

describe('puedeCancelar', () => {
  it('se puede cancelar una confirmada que todavía no empieza', () => {
    expect(puedeCancelar(reserva({}), new Date('2026-11-03T13:59:00Z'))).toBe(true);
  });

  it('no se puede cancelar en el instante en que empieza', () => {
    expect(puedeCancelar(reserva({}), new Date('2026-11-03T14:00:00Z'))).toBe(false);
  });

  it('no se puede cancelar una ya cancelada', () => {
    expect(puedeCancelar(reserva({ estado: 'CANCELADA' }), new Date('2026-11-02T12:00:00Z'))).toBe(false);
  });
});
