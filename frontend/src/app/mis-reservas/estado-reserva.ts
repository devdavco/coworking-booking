import { Reserva } from '../api/modelos';
import { enBogota, formatoFecha, formatoHora } from '../reservar/horas';

export function textoEstado(reserva: Reserva): 'Confirmada' | 'Cancelada' | 'Finalizada' {
  if (reserva.estado === 'CANCELADA') {
    return 'Cancelada';
  }
  return reserva.finalizada ? 'Finalizada' : 'Confirmada';
}

/** El cliente solo puede cancelar reservas confirmadas que todavía no empiezan (lo mismo que exige el backend). */
export function puedeCancelar(reserva: Reserva, ahora: Date): boolean {
  return reserva.estado === 'CONFIRMADA' && Date.parse(reserva.horaInicio) > ahora.getTime();
}

/** "03/11/2026" a partir de la hora de inicio, en Bogotá. */
export function fechaReserva(reserva: Reserva): string {
  return formatoFecha(enBogota(reserva.horaInicio).fecha);
}

/** "09:00 – 10:00": horario elegido por el cliente, sin la limpieza. */
export function horarioReserva(reserva: Reserva): string {
  return `${formatoHora(enBogota(reserva.horaInicio).minutos)} – ${formatoHora(enBogota(reserva.horaFin).minutos)}`;
}
