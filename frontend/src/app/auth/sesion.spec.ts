import { sesionVigente } from './sesion';

describe('sesionVigente', () => {
  it('sin fecha de expiración no hay sesión', () => {
    expect(sesionVigente(null, new Date('2026-11-02T11:59:00Z'))).toBe(false);
  });

  it('es vigente antes de expirar', () => {
    expect(sesionVigente('2026-11-02T12:00:00Z', new Date('2026-11-02T11:59:00Z'))).toBe(true);
  });

  it('ya no es vigente en el instante en que expira', () => {
    expect(sesionVigente('2026-11-02T12:00:00Z', new Date('2026-11-02T12:00:00Z'))).toBe(false);
  });

  it('una fecha inválida no es una sesión vigente', () => {
    expect(sesionVigente('no-es-fecha', new Date('2026-11-02T11:59:00Z'))).toBe(false);
  });
});
