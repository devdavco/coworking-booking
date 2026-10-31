import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, of, switchMap, tap } from 'rxjs';
import { ApiService } from '../api/api.service';
import { Usuario } from '../api/modelos';
import { sesionVigente } from './sesion';

const CLAVE_TOKEN = 'coworking.token';
const CLAVE_EXPIRA = 'coworking.expiraEn';

/** Guarda el token en localStorage y el usuario actual en memoria. */
@Injectable({ providedIn: 'root' })
export class SesionService {
  private readonly api = inject(ApiService);
  private readonly usuarioActual = signal<Usuario | null>(null);

  readonly usuario = this.usuarioActual.asReadonly();
  readonly esAdmin = computed(() => this.usuarioActual()?.rol === 'ADMIN');

  /** El token, o null si no hay sesión o ya expiró. */
  token(): string | null {
    const token = localStorage.getItem(CLAVE_TOKEN);
    return token && sesionVigente(localStorage.getItem(CLAVE_EXPIRA), new Date()) ? token : null;
  }

  iniciar(email: string, password: string): Observable<Usuario> {
    return this.api.login(email, password).pipe(
      tap((respuesta) => {
        localStorage.setItem(CLAVE_TOKEN, respuesta.token);
        localStorage.setItem(CLAVE_EXPIRA, respuesta.expiraEn);
      }),
      switchMap(() => this.api.me()),
      tap((usuario) => this.usuarioActual.set(usuario)),
    );
  }

  cargarUsuario(): Observable<Usuario> {
    const usuario = this.usuarioActual();
    if (usuario) {
      return of(usuario);
    }
    return this.api.me().pipe(tap((cargado) => this.usuarioActual.set(cargado)));
  }

  cerrar(): void {
    localStorage.removeItem(CLAVE_TOKEN);
    localStorage.removeItem(CLAVE_EXPIRA);
    this.usuarioActual.set(null);
  }
}
