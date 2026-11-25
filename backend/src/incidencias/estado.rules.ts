import { EstadoIncidencia as E, Rol } from '../common/enums';

/** Contexto mínimo para decidir permisos sobre una incidencia. */
export interface ContextoIncidencia {
  estado: E;
  reportadoPorId: number;
  tecnicoId: number | null;
}

export interface Actor {
  id: number;
  rol: Rol;
}

export const ESTADOS_FINALES: readonly E[] = [E.CERRADA, E.CANCELADA];
export const ESTADOS_ACTIVOS_TECNICO: readonly E[] = [E.ASIGNADA, E.EN_PROGRESO];

/**
 * Transiciones permitidas mediante PATCH /estado. La asignación (ABIERTA -> ASIGNADA)
 * se realiza exclusivamente con PATCH /asignar.
 */
export function puedeTransicionar(inc: ContextoIncidencia, destino: E, actor: Actor): boolean {
  const esAdmin = actor.rol === Rol.ADMIN;
  const esTecnicoAsignado = actor.rol === Rol.TECNICO && inc.tecnicoId === actor.id;
  const esReportante = inc.reportadoPorId === actor.id;

  switch (`${inc.estado}->${destino}`) {
    case `${E.ASIGNADA}->${E.EN_PROGRESO}`:
      return esTecnicoAsignado || esAdmin;
    case `${E.EN_PROGRESO}->${E.RESUELTA}`:
      return esTecnicoAsignado || esAdmin;
    case `${E.RESUELTA}->${E.CERRADA}`:
      return esReportante || esAdmin;
    case `${E.RESUELTA}->${E.EN_PROGRESO}`: // reapertura
      return esReportante || esAdmin;
  }
  if (destino === E.CANCELADA) {
    return esAdmin && !ESTADOS_FINALES.includes(inc.estado);
  }
  return false;
}

/** Estados a los que el actor puede llevar la incidencia desde su estado actual. */
export function transicionesDisponibles(inc: ContextoIncidencia, actor: Actor): E[] {
  return Object.values(E).filter((d) => d !== inc.estado && puedeTransicionar(inc, d, actor));
}

export function puedeAsignar(inc: ContextoIncidencia, actor: Actor): boolean {
  return actor.rol === Rol.ADMIN && [E.ABIERTA, E.ASIGNADA, E.EN_PROGRESO].includes(inc.estado);
}

export function puedeVer(inc: ContextoIncidencia, actor: Actor): boolean {
  return actor.rol === Rol.ADMIN || inc.reportadoPorId === actor.id || inc.tecnicoId === actor.id;
}

export function puedeEditar(inc: ContextoIncidencia, actor: Actor): boolean {
  if (actor.rol === Rol.ADMIN) return !ESTADOS_FINALES.includes(inc.estado);
  return inc.reportadoPorId === actor.id && inc.estado === E.ABIERTA;
}

export function puedeComentar(inc: ContextoIncidencia, actor: Actor): boolean {
  return puedeVer(inc, actor) && !ESTADOS_FINALES.includes(inc.estado);
}
