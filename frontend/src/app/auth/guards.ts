import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { SesionService } from './sesion.service';

/** Sin sesión vigente, a /login. No llama a la API: un token expirado nunca se envía. */
export const sesionGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(SesionService).token() ? true : router.createUrlTree(['/login']);
};

/**
 * Solo ADMIN; el resto va a /reservar. Es una comodidad de navegación:
 * el backend vuelve a comprobar el rol en cada petición.
 */
export const adminGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(SesionService)
    .cargarUsuario()
    .pipe(
      map((usuario) => (usuario.rol === 'ADMIN' ? true : router.createUrlTree(['/reservar']))),
      catchError(() => of(router.createUrlTree(['/login']))),
    );
};
