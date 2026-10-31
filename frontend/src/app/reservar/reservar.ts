import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { forkJoin, map, of, switchMap } from 'rxjs';
import { ApiService } from '../api/api.service';
import { mensajeDeError } from '../api/errores';
import { ANTICIPACION_DIAS, BLOQUE_MINUTOS, NOMBRE_TIPO } from '../config';
import { Fila, Seleccion, TOTAL_BLOQUES, clicEnBloque, construirFila, inicioBloque, rangoReserva } from './cuadricula';
import { formatoFecha, formatoHora, hoyEnBogota, sumarDias } from './horas';

@Component({
  selector: 'app-reservar',
  templateUrl: './reservar.html',
  styleUrl: './reservar.css',
})
export class Reservar implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly nombreTipo = NOMBRE_TIPO;
  protected readonly hoy = hoyEnBogota(new Date());
  protected readonly maximo = sumarDias(this.hoy, ANTICIPACION_DIAS);
  /** Una etiqueta por hora en punto: 07, 08, … 21. */
  protected readonly horas = Array.from({ length: TOTAL_BLOQUES / 4 }, (_, i) => formatoHora(inicioBloque(i * 4)).slice(0, 2));
  protected readonly indices = Array.from({ length: TOTAL_BLOQUES }, (_, i) => i);

  protected readonly fecha = signal(this.hoy);
  protected readonly filas = signal<Fila[]>([]);
  protected readonly seleccion = signal<Seleccion | null>(null);
  protected readonly cargando = signal(false);
  protected readonly errorCarga = signal('');
  protected readonly confirmacion = signal('');
  protected readonly errorReserva = signal('');
  protected readonly reservando = signal(false);

  protected readonly resumen = computed(() => {
    const sel = this.seleccion();
    const fila = sel && this.filas().find((f) => f.espacio.id === sel.espacioId);
    if (!sel || !fila) {
      return '';
    }
    const desde = formatoHora(inicioBloque(sel.desde));
    const hasta = formatoHora(inicioBloque(sel.hasta) + BLOQUE_MINUTOS);
    return `${fila.espacio.nombre} · ${formatoFecha(this.fecha())} · ${desde} – ${hasta}`;
  });

  ngOnInit(): void {
    this.cargar();
  }

  cambiarFecha(valor: string): void {
    if (!valor) {
      return;
    }
    this.fecha.set(valor);
    this.confirmacion.set('');
    this.errorReserva.set('');
    this.cargar();
  }

  cargar(): void {
    const fecha = this.fecha();
    this.cargando.set(true);
    this.errorCarga.set('');
    this.seleccion.set(null);
    this.api
      .espacios()
      .pipe(
        switchMap((espacios) =>
          espacios.length === 0
            ? of([])
            : forkJoin(
                espacios.map((espacio) =>
                  this.api.ocupacion(espacio.id, fecha).pipe(map((franjas) => construirFila(espacio, franjas, fecha, new Date()))),
                ),
              ),
        ),
      )
      .subscribe({
        next: (filas) => {
          this.filas.set(filas);
          this.cargando.set(false);
        },
        error: (err) => {
          // Sin cuadrícula a medias: o se cargan todos los espacios o se muestra el error.
          this.filas.set([]);
          this.errorCarga.set(mensajeDeError(err));
          this.cargando.set(false);
        },
      });
  }

  clic(fila: Fila, indice: number): void {
    this.confirmacion.set('');
    this.errorReserva.set('');
    this.seleccion.set(clicEnBloque(this.seleccion(), fila, indice));
  }

  seleccionado(fila: Fila, indice: number): boolean {
    const sel = this.seleccion();
    return !!sel && sel.espacioId === fila.espacio.id && indice >= sel.desde && indice <= sel.hasta;
  }

  reservar(): void {
    const sel = this.seleccion();
    if (!sel) {
      return;
    }
    const resumen = this.resumen();
    this.reservando.set(true);
    this.api.crearReserva({ espacioId: sel.espacioId, ...rangoReserva(sel, this.fecha()) }).subscribe({
      next: () => {
        this.confirmacion.set(`Reserva confirmada: ${resumen}`);
        this.reservando.set(false);
        this.cargar();
      },
      error: (err) => {
        this.errorReserva.set(mensajeDeError(err));
        this.reservando.set(false);
        this.cargar();
      },
    });
  }
}
