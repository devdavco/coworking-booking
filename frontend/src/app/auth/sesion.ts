/** La sesión vale mientras su fecha de expiración sea posterior a "ahora". */
export function sesionVigente(expiraEn: string | null, ahora: Date): boolean {
  if (!expiraEn) {
    return false;
  }
  const expira = Date.parse(expiraEn);
  return !Number.isNaN(expira) && expira > ahora.getTime();
}
