import { Component, OnInit, inject, signal } from '@angular/core';
import { Subscription, forkJoin } from 'rxjs';
import { ApiService } from '../api/api.service';
import { mensajeDeError } from '../api/errores';
import { Espacio, Reserva } from '../api/modelos';
import { fechaReserva, horarioReserva, textoEstado } from '../mis-reservas/estado-reserva';
import { hoyEnBogota } from '../reservar/horas';

@Component({
  selector: 'app-admin-reservas',
  templateUrl: './reservas.html',
})
export class AdminReservas implements OnInit {
  private readonly api = inject(ApiService);
  /** Carga en curso; se cancela si cambian los filtros antes de que termine. */
  private carga?: Subscription;

  protected readonly textoEstado = textoEstado;
  protected readonly fechaReserva = fechaReserva;
  protected readonly horarioReserva = horarioReserva;

  /** Fecha vacía = todas las fechas. */
  protected readonly fecha = signal(hoyEnBogota(new Date()));
  /** 0 = todos los espacios. */
  protected readonly espacioId = signal(0);
  protected readonly espacios = signal<Espacio[]>([]);
  protected readonly reservas = signal<Reserva[]>([]);
  protected readonly clientes = signal(new Map<number, string>());
  protected readonly error = signal('');

  ngOnInit(): void {
    forkJoin({ espacios: this.api.espaciosTodos(), usuarios: this.api.usuarios() }).subscribe({
      next: ({ espacios, usuarios }) => {
        this.espacios.set(espacios);
        this.clientes.set(new Map(usuarios.map((u) => [u.id, `${u.nombre} (${u.email})`])));
      },
      error: (err) => this.error.set(mensajeDeError(err)),
    });
    this.cargar();
  }

  cambiarFecha(valor: string): void {
    this.fecha.set(valor);
    this.cargar();
  }

  cambiarEspacio(valor: string): void {
    this.espacioId.set(Number(valor));
    this.cargar();
  }

  cargar(): void {
    this.error.set('');
    this.carga?.unsubscribe();
    this.carga = this.api.reservas({ fecha: this.fecha() || undefined, espacioId: this.espacioId() || undefined }).subscribe({
      next: (reservas) => this.reservas.set(reservas),
      error: (err) => this.error.set(mensajeDeError(err)),
    });
  }

  cliente(reserva: Reserva): string {
    return this.clientes().get(reserva.usuarioId) ?? `Usuario ${reserva.usuarioId}`;
  }

  /** El ADMIN puede cancelar cualquier reserva confirmada, aunque ya haya empezado. */
  cancelar(reserva: Reserva): void {
    if (!confirm(`¿Cancelar la reserva de ${this.cliente(reserva)} en ${reserva.espacioNombre}?`)) {
      return;
    }
    this.error.set('');
    this.api.cancelarReserva(reserva.id).subscribe({
      next: () => this.cargar(),
      error: (err) => this.error.set(mensajeDeError(err)),
    });
  }
}
