import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  CrearReservaRequest,
  Espacio,
  EspacioRequest,
  FranjaOcupada,
  Reserva,
  TokenResponse,
  Usuario,
} from './modelos';

/** Una función por endpoint de la API; no guarda estado. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  private url(ruta: string): string {
    return `${environment.apiUrl}/api${ruta}`;
  }

  login(email: string, password: string): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(this.url('/auth/login'), { email, password });
  }

  registro(nombre: string, email: string, password: string): Observable<Usuario> {
    return this.http.post<Usuario>(this.url('/auth/registro'), { nombre, email, password });
  }

  me(): Observable<Usuario> {
    return this.http.get<Usuario>(this.url('/usuarios/me'));
  }

  usuarios(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(this.url('/usuarios'));
  }

  espacios(): Observable<Espacio[]> {
    return this.http.get<Espacio[]>(this.url('/espacios'));
  }

  espaciosTodos(): Observable<Espacio[]> {
    return this.http.get<Espacio[]>(this.url('/espacios/todos'));
  }

  ocupacion(espacioId: number, fecha: string): Observable<FranjaOcupada[]> {
    return this.http.get<FranjaOcupada[]>(this.url(`/espacios/${espacioId}/ocupacion`), {
      params: { fecha },
    });
  }

  crearEspacio(req: EspacioRequest): Observable<Espacio> {
    return this.http.post<Espacio>(this.url('/espacios'), req);
  }

  actualizarEspacio(id: number, req: EspacioRequest): Observable<Espacio> {
    return this.http.put<Espacio>(this.url(`/espacios/${id}`), req);
  }

  desactivarEspacio(id: number): Observable<void> {
    return this.http.delete<void>(this.url(`/espacios/${id}`));
  }

  activarEspacio(id: number): Observable<Espacio> {
    return this.http.post<Espacio>(this.url(`/espacios/${id}/activar`), null);
  }

  crearReserva(req: CrearReservaRequest): Observable<Reserva> {
    return this.http.post<Reserva>(this.url('/reservas'), req);
  }

  misReservas(): Observable<Reserva[]> {
    return this.http.get<Reserva[]>(this.url('/reservas/mias'));
  }

  /** Solo ADMIN. Los filtros vacíos no se envían. */
  reservas(filtros: { fecha?: string; espacioId?: number }): Observable<Reserva[]> {
    let params = new HttpParams();
    if (filtros.fecha) {
      params = params.set('fecha', filtros.fecha);
    }
    if (filtros.espacioId) {
      params = params.set('espacioId', filtros.espacioId);
    }
    return this.http.get<Reserva[]>(this.url('/reservas'), { params });
  }

  cancelarReserva(id: number): Observable<Reserva> {
    return this.http.post<Reserva>(this.url(`/reservas/${id}/cancelar`), null);
  }
}
