import { Routes } from '@angular/router';
import { Login } from './auth/login';
import { Registro } from './auth/registro';
import { adminGuard, sesionGuard } from './auth/guards';
import { Pendiente } from './pendiente';
import { Reservar } from './reservar/reservar';
import { MisReservas } from './mis-reservas/mis-reservas';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'registro', component: Registro },
  { path: 'reservar', component: Reservar, canActivate: [sesionGuard] },
  { path: 'mis-reservas', component: MisReservas, canActivate: [sesionGuard] },
  { path: 'admin/espacios', component: Pendiente, canActivate: [sesionGuard, adminGuard] },
  { path: 'admin/reservas', component: Pendiente, canActivate: [sesionGuard, adminGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'reservar' },
  { path: '**', redirectTo: 'reservar' },
];
