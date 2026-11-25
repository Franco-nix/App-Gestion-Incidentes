import { BadRequestException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectDataSource, InjectRepository } from '@nestjs/typeorm';
import { Brackets, DataSource, EntityManager, Repository } from 'typeorm';
import { AuthUser } from '../auth/decorators/current-user.decorator';
import { EstadoIncidencia as E, Rol, TipoSeguimiento } from '../common/enums';
import { Categoria } from '../categorias/categoria.entity';
import { Usuario } from '../usuarios/usuario.entity';
import { Incidencia } from './incidencia.entity';
import { Seguimiento } from './seguimiento.entity';
import {
  AsignarDto,
  CambiarEstadoDto,
  CreateIncidenciaDto,
  FiltroIncidenciasDto,
  UpdateIncidenciaDto,
} from './dto/incidencia.dto';
import {
  puedeAsignar,
  puedeComentar,
  puedeEditar,
  puedeTransicionar,
  puedeVer,
  transicionesDisponibles,
} from './estado.rules';

export interface AccionesDisponibles {
  transiciones: E[];
  puedeAsignar: boolean;
  puedeEditar: boolean;
  puedeComentar: boolean;
}

export type IncidenciaDetalle = Incidencia & { seguimientos: Seguimiento[]; acciones: AccionesDisponibles };

export interface Pagina<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

@Injectable()
export class IncidenciasService {
  constructor(
    @InjectRepository(Incidencia) private readonly repo: Repository<Incidencia>,
    @InjectRepository(Seguimiento) private readonly seguimientos: Repository<Seguimiento>,
    @InjectDataSource() private readonly dataSource: DataSource,
  ) {}

  // ---------- Registro ----------

  async create(dto: CreateIncidenciaDto, actor: AuthUser): Promise<IncidenciaDetalle> {
    const id = await this.dataSource.transaction(async (m) => {
      await this.assertCategoriaActiva(m, dto.categoriaId);
      // Un solo INSERT atómico: el código legible (INC-2026-00042) se deriva del id de la secuencia
      const [{ id }]: { id: number }[] = await m.query(
        `INSERT INTO incidencias (id, codigo, titulo, descripcion, ubicacion, categoria_id, prioridad, estado, reportado_por)
         SELECT s.id, 'INC-' || to_char(now(), 'YYYY') || '-' || lpad(s.id::text, 5, '0'), $1, $2, $3, $4, $5, 'ABIERTA', $6
         FROM (SELECT nextval(pg_get_serial_sequence('incidencias', 'id'))::int AS id) s
         RETURNING id`,
        [dto.titulo.trim(), dto.descripcion.trim(), dto.ubicacion?.trim() || null, dto.categoriaId, dto.prioridad, actor.id],
      );
      await this.registrar(m, id, actor.id, TipoSeguimiento.CREACION, {
        estadoNuevo: E.ABIERTA,
        comentario: 'Incidencia registrada',
      });
      return id;
    });
    return this.findOne(id, actor);
  }

  async update(id: number, dto: UpdateIncidenciaDto, actor: AuthUser): Promise<IncidenciaDetalle> {
    const inc = await this.getOrFail(id);
    if (!puedeEditar(inc, actor)) {
      throw new ForbiddenException('Solo puedes editar incidencias propias que aún no han sido asignadas');
    }
    if (dto.categoriaId && dto.categoriaId !== inc.categoriaId) {
      await this.assertCategoriaActiva(this.repo.manager, dto.categoriaId);
    }
    await this.repo.update(id, {
      ...(dto.titulo !== undefined && { titulo: dto.titulo.trim() }),
      ...(dto.descripcion !== undefined && { descripcion: dto.descripcion.trim() }),
      ...(dto.ubicacion !== undefined && { ubicacion: dto.ubicacion.trim() || null }),
      ...(dto.categoriaId !== undefined && { categoriaId: dto.categoriaId }),
      ...(dto.prioridad !== undefined && { prioridad: dto.prioridad }),
    });
    return this.findOne(id, actor);
  }

  // ---------- Consulta ----------

  async findAll(f: FiltroIncidenciasDto, actor: AuthUser): Promise<Pagina<Incidencia>> {
    const qb = this.repo
      .createQueryBuilder('i')
      .leftJoinAndSelect('i.categoria', 'categoria')
      .leftJoinAndSelect('i.reportadoPor', 'reportadoPor')
      .leftJoinAndSelect('i.tecnico', 'tecnico');

    // Alcance según rol
    if (actor.rol === Rol.USUARIO) qb.andWhere('i.reportado_por = :uid', { uid: actor.id });
    if (actor.rol === Rol.TECNICO) qb.andWhere('i.tecnico_id = :uid', { uid: actor.id });

    if (f.estado) qb.andWhere('i.estado = :estado', { estado: f.estado });
    if (f.prioridad) qb.andWhere('i.prioridad = :prioridad', { prioridad: f.prioridad });
    if (f.categoriaId) qb.andWhere('i.categoria_id = :cat', { cat: f.categoriaId });
    if (f.tecnicoId && actor.rol === Rol.ADMIN) qb.andWhere('i.tecnico_id = :tec', { tec: f.tecnicoId });
    if (f.sinAsignar) qb.andWhere('i.tecnico_id IS NULL');
    if (f.q?.trim()) {
      const q = `%${f.q.trim()}%`;
      qb.andWhere(
        new Brackets((w) =>
          w.where('i.codigo ILIKE :q', { q }).orWhere('i.titulo ILIKE :q', { q }).orWhere('i.descripcion ILIKE :q', { q }),
        ),
      );
    }

    // Orden: las activas y más urgentes primero, luego las más recientes
    qb.addSelect(
      `CASE i.estado WHEN 'CERRADA' THEN 2 WHEN 'CANCELADA' THEN 2 WHEN 'RESUELTA' THEN 1 ELSE 0 END`,
      'orden_estado',
    )
      .addSelect(
        `CASE i.prioridad WHEN 'CRITICA' THEN 0 WHEN 'ALTA' THEN 1 WHEN 'MEDIA' THEN 2 ELSE 3 END`,
        'orden_prioridad',
      )
      .orderBy('orden_estado', 'ASC')
      .addOrderBy('orden_prioridad', 'ASC')
      .addOrderBy('i.fechaCreacion', 'DESC')
      .skip((f.page - 1) * f.size)
      .take(f.size);

    const [items, total] = await qb.getManyAndCount();
    return { items, total, page: f.page, size: f.size };
  }

  async findOne(id: number, actor: AuthUser): Promise<IncidenciaDetalle> {
    const inc = await this.getOrFail(id);
    if (!puedeVer(inc, actor)) throw new ForbiddenException('No tienes acceso a esta incidencia');
    const seguimientos = await this.seguimientos.find({
      where: { incidenciaId: id },
      order: { createdAt: 'ASC', id: 'ASC' },
    });
    return Object.assign(inc, { seguimientos, acciones: this.acciones(inc, actor) });
  }

  // ---------- Asignación ----------

  async asignar(id: number, dto: AsignarDto, actor: AuthUser): Promise<IncidenciaDetalle> {
    await this.dataSource.transaction(async (m) => {
      const inc = await this.lockOrFail(m, id);
      if (!puedeAsignar(inc, actor)) {
        throw new BadRequestException(`No se puede asignar una incidencia en estado ${inc.estado}`);
      }
      const tecnico = await m.findOneBy(Usuario, { id: dto.tecnicoId });
      if (!tecnico || tecnico.rol !== Rol.TECNICO || !tecnico.activo) {
        throw new BadRequestException('El técnico seleccionado no existe o no está activo');
      }
      if (inc.tecnicoId === tecnico.id) {
        throw new BadRequestException('La incidencia ya está asignada a ese técnico');
      }

      const reasignacion = inc.tecnicoId !== null;
      const estadoAnterior = inc.estado;
      const estadoNuevo = inc.estado === E.ABIERTA ? E.ASIGNADA : inc.estado;
      await m.update(Incidencia, id, {
        tecnicoId: tecnico.id,
        estado: estadoNuevo,
        fechaAsignacion: new Date(),
      });
      const base = `${reasignacion ? 'Reasignada' : 'Asignada'} a ${tecnico.nombre}`;
      await this.registrar(m, id, actor.id, TipoSeguimiento.ASIGNACION, {
        estadoAnterior,
        estadoNuevo,
        comentario: dto.comentario?.trim() ? `${base}. ${dto.comentario.trim()}` : base,
      });
    });
    return this.findOne(id, actor);
  }

  // ---------- Seguimiento ----------

  async cambiarEstado(id: number, dto: CambiarEstadoDto, actor: AuthUser): Promise<IncidenciaDetalle> {
    await this.dataSource.transaction(async (m) => {
      const inc = await this.lockOrFail(m, id);
      if (!puedeVer(inc, actor)) throw new ForbiddenException('No tienes acceso a esta incidencia');
      if (!puedeTransicionar(inc, dto.estado, actor)) {
        throw new BadRequestException(`No puedes cambiar el estado de ${inc.estado} a ${dto.estado}`);
      }
      if (dto.estado === E.RESUELTA && !dto.comentario?.trim()) {
        throw new BadRequestException('Describe la solución aplicada al marcarla como resuelta');
      }

      const cambios: Partial<Incidencia> = { estado: dto.estado };
      if (dto.estado === E.RESUELTA) cambios.fechaResolucion = new Date();
      if (dto.estado === E.CERRADA || dto.estado === E.CANCELADA) cambios.fechaCierre = new Date();
      if (inc.estado === E.RESUELTA && dto.estado === E.EN_PROGRESO) cambios.fechaResolucion = null; // reapertura

      await m.update(Incidencia, id, cambios);
      await this.registrar(m, id, actor.id, TipoSeguimiento.CAMBIO_ESTADO, {
        estadoAnterior: inc.estado,
        estadoNuevo: dto.estado,
        comentario: dto.comentario?.trim() || null,
      });
    });
    return this.findOne(id, actor);
  }

  async comentar(id: number, comentario: string, actor: AuthUser): Promise<Seguimiento> {
    const inc = await this.getOrFail(id);
    if (!puedeComentar(inc, actor)) throw new ForbiddenException('No puedes comentar en esta incidencia');
    const s = await this.registrar(this.repo.manager, id, actor.id, TipoSeguimiento.COMENTARIO, {
      comentario: comentario.trim(),
    });
    await this.repo.update(id, { updatedAt: new Date() });
    return this.seguimientos.findOneOrFail({ where: { id: s.id } });
  }

  // ---------- Helpers ----------

  acciones(inc: Incidencia, actor: AuthUser): AccionesDisponibles {
    return {
      transiciones: transicionesDisponibles(inc, actor),
      puedeAsignar: puedeAsignar(inc, actor),
      puedeEditar: puedeEditar(inc, actor),
      puedeComentar: puedeComentar(inc, actor),
    };
  }

  private async getOrFail(id: number): Promise<Incidencia> {
    const inc = await this.repo.findOneBy({ id });
    if (!inc) throw new NotFoundException('Incidencia no encontrada');
    return inc;
  }

  /** Bloquea la fila durante la transacción para evitar cambios concurrentes. */
  private async lockOrFail(m: EntityManager, id: number): Promise<Incidencia> {
    const inc = await m
      .createQueryBuilder(Incidencia, 'i')
      .setLock('pessimistic_write')
      .where('i.id = :id', { id })
      .getOne();
    if (!inc) throw new NotFoundException('Incidencia no encontrada');
    return inc;
  }

  private async assertCategoriaActiva(m: EntityManager, categoriaId: number) {
    const cat = await m.findOneBy(Categoria, { id: categoriaId });
    if (!cat || !cat.activo) throw new BadRequestException('Categoría inválida');
  }

  private registrar(
    m: EntityManager,
    incidenciaId: number,
    usuarioId: number,
    tipo: TipoSeguimiento,
    datos: { estadoAnterior?: E | null; estadoNuevo?: E | null; comentario?: string | null },
  ): Promise<Seguimiento> {
    return m.save(
      m.create(Seguimiento, {
        incidenciaId,
        usuarioId,
        tipo,
        estadoAnterior: datos.estadoAnterior ?? null,
        estadoNuevo: datos.estadoNuevo ?? null,
        comentario: datos.comentario ?? null,
      }),
    );
  }
}
