import { Routes } from '@angular/router';
import { AdminEspacios } from './admin/espacios';
import { AdminReservas } from './admin/reservas';
import { Login } from './auth/login';
import { Registro } from './auth/registro';
import { adminGuard, sesionGuard } from './auth/guards';
import { MisReservas } from './mis-reservas/mis-reservas';
import { Reservar } from './reservar/reservar';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'registro', component: Registro },
  { path: 'reservar', component: Reservar, canActivate: [sesionGuard] },
  { path: 'mis-reservas', component: MisReservas, canActivate: [sesionGuard] },
  { path: 'admin/espacios', component: AdminEspacios, canActivate: [sesionGuard, adminGuard] },
  { path: 'admin/reservas', component: AdminReservas, canActivate: [sesionGuard, adminGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'reservar' },
  { path: '**', redirectTo: 'reservar' },
];
