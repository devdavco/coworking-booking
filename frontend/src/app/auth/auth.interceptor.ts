import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { SesionService } from './sesion.service';

/** Agrega el token a las peticiones a la API y, si el backend lo rechaza, cierra la sesión. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const esApi = req.url.startsWith(environment.apiUrl);
  const esAuth = req.url.includes('/api/auth/');
  const sesion = inject(SesionService);
  const router = inject(Router);
  const token = esApi && !esAuth ? sesion.token() : null;

  const peticion = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  return next(peticion).pipe(
    catchError((err: unknown) => {
      if (token && err instanceof HttpErrorResponse && err.status === 401) {
        sesion.cerrar();
        router.navigate(['/login'], { queryParams: { expirada: 1 } });
      }
      return throwError(() => err);
    }),
  );
};
