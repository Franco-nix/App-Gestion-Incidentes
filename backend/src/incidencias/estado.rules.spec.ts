import { EstadoIncidencia as E, Rol } from '../common/enums';
import {
  ContextoIncidencia,
  puedeAsignar,
  puedeComentar,
  puedeEditar,
  puedeTransicionar,
  puedeVer,
  transicionesDisponibles,
} from './estado.rules';

const ADMIN = { id: 1, rol: Rol.ADMIN };
const TECNICO = { id: 2, rol: Rol.TECNICO };
const OTRO_TECNICO = { id: 3, rol: Rol.TECNICO };
const REPORTANTE = { id: 5, rol: Rol.USUARIO };
const OTRO_USUARIO = { id: 6, rol: Rol.USUARIO };

const inc = (estado: E, tecnicoId: number | null = TECNICO.id): ContextoIncidencia => ({
  estado,
  reportadoPorId: REPORTANTE.id,
  tecnicoId: estado === E.ABIERTA ? null : tecnicoId,
});

describe('estado.rules', () => {
  describe('puedeTransicionar', () => {
    it.each([
      // [estado, destino, actor, esperado]
      [E.ASIGNADA, E.EN_PROGRESO, TECNICO, true],
      [E.ASIGNADA, E.EN_PROGRESO, ADMIN, true],
      [E.ASIGNADA, E.EN_PROGRESO, OTRO_TECNICO, false],
      [E.ASIGNADA, E.EN_PROGRESO, REPORTANTE, false],
      [E.EN_PROGRESO, E.RESUELTA, TECNICO, true],
      [E.EN_PROGRESO, E.RESUELTA, REPORTANTE, false],
      [E.RESUELTA, E.CERRADA, REPORTANTE, true],
      [E.RESUELTA, E.CERRADA, ADMIN, true],
      [E.RESUELTA, E.CERRADA, TECNICO, false],
      [E.RESUELTA, E.CERRADA, OTRO_USUARIO, false],
      [E.RESUELTA, E.EN_PROGRESO, REPORTANTE, true], // reapertura
      [E.RESUELTA, E.EN_PROGRESO, TECNICO, false],
      [E.ABIERTA, E.CANCELADA, ADMIN, true],
      [E.EN_PROGRESO, E.CANCELADA, ADMIN, true],
      [E.ABIERTA, E.CANCELADA, REPORTANTE, false],
      [E.CERRADA, E.CANCELADA, ADMIN, false],
      [E.CANCELADA, E.ABIERTA, ADMIN, false],
      // La asignación solo ocurre vía /asignar
      [E.ABIERTA, E.ASIGNADA, ADMIN, false],
      // No se pueden saltar pasos
      [E.ASIGNADA, E.RESUELTA, TECNICO, false],
      [E.ABIERTA, E.EN_PROGRESO, ADMIN, false],
      [E.EN_PROGRESO, E.CERRADA, ADMIN, false],
    ])('%s -> %s por %o = %s', (estado, destino, actor, esperado) => {
      expect(puedeTransicionar(inc(estado), destino, actor)).toBe(esperado);
    });
  });

  it('transicionesDisponibles lista solo los destinos válidos', () => {
    expect(transicionesDisponibles(inc(E.ASIGNADA), TECNICO)).toEqual([E.EN_PROGRESO]);
    expect(transicionesDisponibles(inc(E.ASIGNADA), ADMIN)).toEqual([E.EN_PROGRESO, E.CANCELADA]);
    expect(transicionesDisponibles(inc(E.RESUELTA), REPORTANTE)).toEqual([E.EN_PROGRESO, E.CERRADA]);
    expect(transicionesDisponibles(inc(E.CERRADA), ADMIN)).toEqual([]);
  });

  it('solo el admin asigna, y solo en estados no terminados', () => {
    expect(puedeAsignar(inc(E.ABIERTA), ADMIN)).toBe(true);
    expect(puedeAsignar(inc(E.EN_PROGRESO), ADMIN)).toBe(true); // reasignación
    expect(puedeAsignar(inc(E.RESUELTA), ADMIN)).toBe(false);
    expect(puedeAsignar(inc(E.ABIERTA), TECNICO)).toBe(false);
  });

  it('visibilidad por rol', () => {
    expect(puedeVer(inc(E.ASIGNADA), ADMIN)).toBe(true);
    expect(puedeVer(inc(E.ASIGNADA), TECNICO)).toBe(true);
    expect(puedeVer(inc(E.ASIGNADA), REPORTANTE)).toBe(true);
    expect(puedeVer(inc(E.ASIGNADA), OTRO_TECNICO)).toBe(false);
    expect(puedeVer(inc(E.ASIGNADA), OTRO_USUARIO)).toBe(false);
  });

  it('el reportante solo edita mientras está ABIERTA', () => {
    expect(puedeEditar(inc(E.ABIERTA), REPORTANTE)).toBe(true);
    expect(puedeEditar(inc(E.ASIGNADA), REPORTANTE)).toBe(false);
    expect(puedeEditar(inc(E.ABIERTA), OTRO_USUARIO)).toBe(false);
    expect(puedeEditar(inc(E.EN_PROGRESO), ADMIN)).toBe(true);
    expect(puedeEditar(inc(E.CERRADA), ADMIN)).toBe(false);
  });

  it('no se comenta en incidencias finalizadas', () => {
    expect(puedeComentar(inc(E.RESUELTA), REPORTANTE)).toBe(true);
    expect(puedeComentar(inc(E.CERRADA), REPORTANTE)).toBe(false);
    expect(puedeComentar(inc(E.ASIGNADA), OTRO_USUARIO)).toBe(false);
  });
});
