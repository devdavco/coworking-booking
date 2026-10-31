import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../api/api.service';
import { mensajeDeError } from '../api/errores';
import { Reserva } from '../api/modelos';
import { fechaReserva, horarioReserva, puedeCancelar, textoEstado } from './estado-reserva';

@Component({
  selector: 'app-mis-reservas',
  imports: [RouterLink],
  templateUrl: './mis-reservas.html',
})
export class MisReservas implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly textoEstado = textoEstado;
  protected readonly fechaReserva = fechaReserva;
  protected readonly horarioReserva = horarioReserva;

  protected readonly reservas = signal<Reserva[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal('');

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.api.misReservas().subscribe({
      next: (reservas) => {
        this.reservas.set(reservas);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(mensajeDeError(err));
        this.cargando.set(false);
      },
    });
  }

  puedeCancelar(reserva: Reserva): boolean {
    return puedeCancelar(reserva, new Date());
  }

  cancelar(reserva: Reserva): void {
    if (!confirm(`¿Cancelar la reserva de ${reserva.espacioNombre} del ${fechaReserva(reserva)}, ${horarioReserva(reserva)}?`)) {
      return;
    }
    this.error.set('');
    this.api.cancelarReserva(reserva.id).subscribe({
      next: () => this.cargar(),
      error: (err) => this.error.set(mensajeDeError(err)),
    });
  }
}
