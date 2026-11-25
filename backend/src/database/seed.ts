/**
 * Carga datos de demostración. ¡BORRA todos los datos existentes!
 * Uso: npm run seed
 */
import 'reflect-metadata';
import 'dotenv/config';
import * as bcrypt from 'bcryptjs';
import { DataSource, EntityManager } from 'typeorm';
import { buildDataSourceOptions } from '../config/database.config';
import { EstadoIncidencia as E, Prioridad as P, Rol, TipoSeguimiento as T } from '../common/enums';
import { Usuario } from '../usuarios/usuario.entity';
import { Categoria } from '../categorias/categoria.entity';
import { Incidencia } from '../incidencias/incidencia.entity';
import { Seguimiento } from '../incidencias/seguimiento.entity';

// PRNG determinista para que el seed sea reproducible
let semilla = 20261123;
const rnd = () => ((semilla = (semilla * 1103515245 + 12345) % 2 ** 31) / 2 ** 31);
const elegir = <T>(arr: readonly T[]): T => arr[Math.floor(rnd() * arr.length)];
const horas = (h: number) => h * 3600 * 1000;

const CATEGORIAS = [
  ['Hardware', 'Equipos de cómputo, periféricos y componentes'],
  ['Software', 'Sistemas operativos, aplicaciones y licencias'],
  ['Red e Internet', 'Conectividad, Wi-Fi, cableado y VPN'],
  ['Impresoras', 'Impresoras, escáneres y multifuncionales'],
  ['Accesos y cuentas', 'Contraseñas, permisos y correo institucional'],
  ['Otros', 'Incidencias que no encajan en otra categoría'],
] as const;

const PLANTILLAS: Record<string, [string, string][]> = {
  Hardware: [
    ['PC no enciende', 'El equipo de escritorio no muestra señal al presionar el botón de encendido.'],
    ['Monitor con parpadeo', 'El monitor parpadea constantemente y a veces se apaga solo.'],
    ['Teclado con teclas dañadas', 'Varias teclas no responden, entre ellas la barra espaciadora.'],
    ['Proyector sin imagen', 'El proyector del aula enciende pero no muestra imagen desde la laptop.'],
  ],
  Software: [
    ['Office solicita activación', 'Al abrir Word aparece un mensaje de licencia no activada.'],
    ['Sistema académico lento', 'El sistema de notas tarda más de un minuto en cargar cada página.'],
    ['Instalar software estadístico', 'Se requiere instalar el software estadístico para el curso de investigación.'],
    ['Antivirus desactualizado', 'El antivirus muestra alerta de definiciones desactualizadas.'],
  ],
  'Red e Internet': [
    ['Sin conexión a Internet', 'La oficina no tiene conexión a Internet desde esta mañana.'],
    ['Wi-Fi intermitente', 'La red Wi-Fi del pabellón se desconecta cada pocos minutos.'],
    ['Punto de red sin servicio', 'El punto de red de la pared no da conectividad al equipo.'],
    ['VPN no conecta', 'No es posible conectarse a la VPN institucional desde casa.'],
  ],
  Impresoras: [
    ['Impresora atasca papel', 'La impresora multifuncional atasca el papel en cada impresión.'],
    ['No imprime desde la red', 'Los equipos no encuentran la impresora compartida en la red.'],
    ['Escáner no envía correos', 'La función escanear a correo electrónico devuelve error.'],
  ],
  'Accesos y cuentas': [
    ['Restablecer contraseña de correo', 'Olvidé la contraseña del correo institucional y no puedo ingresar.'],
    ['Acceso a carpeta compartida', 'Necesito permisos de lectura en la carpeta compartida de la oficina.'],
    ['Cuenta bloqueada', 'Mi cuenta se bloqueó tras varios intentos fallidos de inicio de sesión.'],
  ],
  Otros: [
    ['Cableado expuesto', 'Hay cables de red sueltos en el pasillo que representan un riesgo.'],
    ['Solicitud de equipo para evento', 'Se requiere laptop y parlantes para un evento en el auditorio.'],
  ],
};

const UBICACIONES = [
  'Pabellón A - Aula 101', 'Pabellón A - Aula 204', 'Pabellón B - Laboratorio 3', 'Biblioteca central',
  'Secretaría académica', 'Dirección', 'Contabilidad', 'Auditorio principal', 'Sala de docentes', 'Recursos humanos',
];

const SOLUCIONES = [
  'Se reemplazó el componente defectuoso y se verificó el funcionamiento.',
  'Se reinstaló el controlador y se realizaron pruebas con el usuario.',
  'Se reconfiguró el equipo y se restableció el servicio.',
  'Se actualizó el software a la última versión y se validó con el usuario.',
  'Se restableció la credencial y se confirmó el acceso.',
];

async function main() {
  const ds = new DataSource(buildDataSourceOptions());
  await ds.initialize();
  console.log('Limpiando tablas...');
  await ds.query('TRUNCATE seguimientos, incidencias, categorias, usuarios RESTART IDENTITY CASCADE');

  await ds.transaction(async (m) => {
    const usuarios = await crearUsuarios(m);
    const categorias = await m.save(
      Categoria,
      CATEGORIAS.map(([nombre, descripcion]) => m.create(Categoria, { nombre, descripcion })),
    );
    await crearIncidencias(m, usuarios, categorias);
  });

  const [{ n }] = await ds.query('SELECT COUNT(*)::int AS n FROM incidencias');
  console.log(`Seed completado: ${n} incidencias.`);
  console.log('\nCredenciales de prueba:');
  console.log('  ADMIN    admin@inst.edu       / Admin123*');
  console.log('  TECNICO  carlos.ruiz@inst.edu / Tecnico123*  (también lucia.mendoza@, jorge.paredes@)');
  console.log('  USUARIO  maria.lopez@inst.edu / Usuario123*  (también pedro.quispe@, ana.torres@)');
  await ds.destroy();
}

async function crearUsuarios(m: EntityManager) {
  const [hAdmin, hTec, hUsr] = await Promise.all([
    bcrypt.hash('Admin123*', 10),
    bcrypt.hash('Tecnico123*', 10),
    bcrypt.hash('Usuario123*', 10),
  ]);
  const datos: Partial<Usuario>[] = [
    { nombre: 'Administrador TI', email: 'admin@inst.edu', passwordHash: hAdmin, rol: Rol.ADMIN, area: 'Oficina de TI' },
    { nombre: 'Carlos Ruiz', email: 'carlos.ruiz@inst.edu', passwordHash: hTec, rol: Rol.TECNICO, area: 'Oficina de TI', especialidad: 'Hardware y redes' },
    { nombre: 'Lucía Mendoza', email: 'lucia.mendoza@inst.edu', passwordHash: hTec, rol: Rol.TECNICO, area: 'Oficina de TI', especialidad: 'Software y cuentas' },
    { nombre: 'Jorge Paredes', email: 'jorge.paredes@inst.edu', passwordHash: hTec, rol: Rol.TECNICO, area: 'Oficina de TI', especialidad: 'Soporte general' },
    { nombre: 'María López', email: 'maria.lopez@inst.edu', passwordHash: hUsr, rol: Rol.USUARIO, area: 'Secretaría académica' },
    { nombre: 'Pedro Quispe', email: 'pedro.quispe@inst.edu', passwordHash: hUsr, rol: Rol.USUARIO, area: 'Docente - Ingeniería' },
    { nombre: 'Ana Torres', email: 'ana.torres@inst.edu', passwordHash: hUsr, rol: Rol.USUARIO, area: 'Contabilidad' },
  ];
  const guardados = await m.save(Usuario, datos.map((d) => m.create(Usuario, d)));
  return {
    admin: guardados[0],
    tecnicos: guardados.filter((u) => u.rol === Rol.TECNICO),
    usuarios: guardados.filter((u) => u.rol === Rol.USUARIO),
  };
}

async function crearIncidencias(
  m: EntityManager,
  u: { admin: Usuario; tecnicos: Usuario[]; usuarios: Usuario[] },
  categorias: Categoria[],
) {
  const ahora = Date.now();
  const TOTAL = 42;
  const HORAS_POR_PRIORIDAD: Record<P, number> = { CRITICA: 4, ALTA: 12, MEDIA: 30, BAJA: 60 };

  for (let k = 0; k < TOTAL; k++) {
    // Más antiguas primero; repartidas en ~70 días
    const creada = new Date(ahora - horas(24 * 70 * (1 - k / TOTAL)) - horas(rnd() * 8));
    const cat = elegir(categorias);
    const [titulo, descripcion] = elegir(PLANTILLAS[cat.nombre]);
    const prioridad = elegir([P.BAJA, P.MEDIA, P.MEDIA, P.ALTA, P.ALTA, P.CRITICA]);
    const reportante = elegir(u.usuarios);
    const edadDias = (ahora - creada.getTime()) / horas(24);

    // Estado objetivo según la antigüedad: las viejas casi siempre terminaron
    let destino: E;
    const r = rnd();
    if (k >= TOTAL - 4) destino = E.ABIERTA; // las más recientes quedan pendientes de asignar
    else if (edadDias > 14) destino = r < 0.75 ? E.CERRADA : r < 0.9 ? E.RESUELTA : E.CANCELADA;
    else if (edadDias > 4) destino = elegir([E.CERRADA, E.RESUELTA, E.EN_PROGRESO, E.ASIGNADA]);
    else destino = elegir([E.ABIERTA, E.ABIERTA, E.ASIGNADA, E.EN_PROGRESO, E.RESUELTA]);

    const n = k + 1;
    const segs: Partial<Seguimiento>[] = [];
    const inc: Partial<Incidencia> = {
      codigo: `INC-${creada.getFullYear()}-${String(n).padStart(5, '0')}`,
      titulo,
      descripcion,
      ubicacion: elegir(UBICACIONES),
      categoriaId: cat.id,
      prioridad,
      estado: E.ABIERTA,
      reportadoPorId: reportante.id,
      tecnicoId: null,
      fechaCreacion: creada,
    };
    segs.push({ usuarioId: reportante.id, tipo: T.CREACION, estadoNuevo: E.ABIERTA, comentario: 'Incidencia registrada', createdAt: creada });

    let t = creada.getTime();
    const avanzar = (h: number) => Math.min((t += horas(h)), ahora - horas(0.5));

    if (destino === E.CANCELADA) {
      const f = new Date(avanzar(2 + rnd() * 20));
      Object.assign(inc, { estado: E.CANCELADA, fechaCierre: f });
      segs.push({ usuarioId: u.admin.id, tipo: T.CAMBIO_ESTADO, estadoAnterior: E.ABIERTA, estadoNuevo: E.CANCELADA, comentario: 'Duplicada / resuelta por el usuario', createdAt: f });
    } else if (destino !== E.ABIERTA) {
      const tec = elegir(u.tecnicos);
      const fa = new Date(avanzar(0.5 + rnd() * 6));
      Object.assign(inc, { estado: E.ASIGNADA, tecnicoId: tec.id, fechaAsignacion: fa });
      segs.push({ usuarioId: u.admin.id, tipo: T.ASIGNACION, estadoAnterior: E.ABIERTA, estadoNuevo: E.ASIGNADA, comentario: `Asignada a ${tec.nombre}`, createdAt: fa });

      if ([E.EN_PROGRESO, E.RESUELTA, E.CERRADA].includes(destino)) {
        const fp = new Date(avanzar(0.3 + rnd() * 3));
        inc.estado = E.EN_PROGRESO;
        segs.push({ usuarioId: tec.id, tipo: T.CAMBIO_ESTADO, estadoAnterior: E.ASIGNADA, estadoNuevo: E.EN_PROGRESO, comentario: 'Revisando el caso', createdAt: fp });
        if (rnd() < 0.4) {
          segs.push({ usuarioId: reportante.id, tipo: T.COMENTARIO, comentario: 'Gracias, quedo atento.', createdAt: new Date(avanzar(0.5)) });
        }
      }
      if ([E.RESUELTA, E.CERRADA].includes(destino)) {
        const fr = new Date(avanzar(HORAS_POR_PRIORIDAD[prioridad] * (0.4 + rnd())));
        Object.assign(inc, { estado: E.RESUELTA, fechaResolucion: fr });
        segs.push({ usuarioId: tec.id, tipo: T.CAMBIO_ESTADO, estadoAnterior: E.EN_PROGRESO, estadoNuevo: E.RESUELTA, comentario: elegir(SOLUCIONES), createdAt: fr });
      }
      if (destino === E.CERRADA) {
        const fc = new Date(avanzar(1 + rnd() * 24));
        Object.assign(inc, { estado: E.CERRADA, fechaCierre: fc });
        segs.push({ usuarioId: reportante.id, tipo: T.CAMBIO_ESTADO, estadoAnterior: E.RESUELTA, estadoNuevo: E.CERRADA, comentario: 'Conforme con la solución', createdAt: fc });
      }
    }

    const { identifiers } = await m.insert(Incidencia, { ...inc, updatedAt: new Date(t) });
    const id = identifiers[0].id as number;
    await m.insert(Seguimiento, segs.map((s) => ({ ...s, incidenciaId: id })));
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
