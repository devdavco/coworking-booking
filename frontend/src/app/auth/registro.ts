import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { ApiService } from '../api/api.service';
import { erroresPorCampo, mensajeDeError } from '../api/errores';
import { SesionService } from './sesion.service';

@Component({
  selector: 'app-registro',
  imports: [FormsModule, RouterLink],
  templateUrl: './registro.html',
})
export class Registro {
  private readonly api = inject(ApiService);
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);

  nombre = '';
  email = '';
  password = '';
  readonly error = signal('');
  readonly errores = signal<Record<string, string>>({});
  readonly enviando = signal(false);

  registrar(): void {
    this.error.set('');
    this.errores.set({});
    this.enviando.set(true);
    this.api
      .registro(this.nombre, this.email, this.password)
      .pipe(switchMap(() => this.sesion.iniciar(this.email, this.password)))
      .subscribe({
        next: () => this.router.navigate(['/reservar']),
        error: (err) => {
          const porCampo = erroresPorCampo(err);
          this.errores.set(porCampo);
          this.error.set(Object.keys(porCampo).length ? '' : mensajeDeError(err));
          this.enviando.set(false);
        },
      });
  }
}
