import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../api/api.service';
import { erroresPorCampo, mensajeDeError } from '../api/errores';
import { Espacio, EspacioRequest } from '../api/modelos';
import { NOMBRE_TIPO, TIPOS_ESPACIO } from '../config';

function formularioVacio(): EspacioRequest {
  return { nombre: '', tipo: 'SALA_REUNIONES', capacidad: 1, minutosLimpieza: 0 };
}

@Component({
  selector: 'app-admin-espacios',
  imports: [FormsModule],
  templateUrl: './espacios.html',
})
export class AdminEspacios implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly tipos = TIPOS_ESPACIO;
  protected readonly nombreTipo = NOMBRE_TIPO;

  protected readonly espacios = signal<Espacio[]>([]);
  /** null: el formulario crea; un espacio: el formulario edita ese espacio. */
  protected readonly editando = signal<Espacio | null>(null);
  protected formulario: EspacioRequest = formularioVacio();
  protected readonly error = signal('');
  protected readonly errores = signal<Record<string, string>>({});
  protected readonly guardando = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.api.espaciosTodos().subscribe({
      next: (espacios) => this.espacios.set(espacios),
      error: (err) => this.error.set(mensajeDeError(err)),
    });
  }

  editar(espacio: Espacio): void {
    this.limpiarErrores();
    this.editando.set(espacio);
    this.formulario = {
      nombre: espacio.nombre,
      tipo: espacio.tipo,
      capacidad: espacio.capacidad,
      minutosLimpieza: espacio.minutosLimpieza,
    };
  }

  cancelarEdicion(): void {
    this.limpiarErrores();
    this.editando.set(null);
    this.formulario = formularioVacio();
  }

  guardar(): void {
    this.limpiarErrores();
    this.guardando.set(true);
    const espacio = this.editando();
    const peticion = espacio
      ? this.api.actualizarEspacio(espacio.id, this.formulario)
      : this.api.crearEspacio(this.formulario);
    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.cancelarEdicion();
        this.cargar();
      },
      error: (err) => {
        const porCampo = erroresPorCampo(err);
        this.errores.set(porCampo);
        this.error.set(Object.keys(porCampo).length ? '' : mensajeDeError(err));
        this.guardando.set(false);
      },
    });
  }

  desactivar(espacio: Espacio): void {
    if (!confirm(`¿Desactivar "${espacio.nombre}"? Dejará de aparecer para reservar.`)) {
      return;
    }
    this.limpiarErrores();
    this.api.desactivarEspacio(espacio.id).subscribe({
      next: () => this.cargar(),
      error: (err) => this.error.set(mensajeDeError(err)),
    });
  }

  reactivar(espacio: Espacio): void {
    this.limpiarErrores();
    this.api.activarEspacio(espacio.id).subscribe({
      next: () => this.cargar(),
      error: (err) => this.error.set(mensajeDeError(err)),
    });
  }

  private limpiarErrores(): void {
    this.error.set('');
    this.errores.set({});
  }
}
