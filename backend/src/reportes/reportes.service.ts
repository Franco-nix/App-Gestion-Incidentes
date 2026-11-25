import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, SelectQueryBuilder } from 'typeorm';
import { EstadoIncidencia as E, Rol } from '../common/enums';
import { Incidencia } from '../incidencias/incidencia.entity';
import { ExportDto, RangoFechasDto, TendenciaDto } from './dto/rango.dto';

export interface Conteo {
  clave: string;
  total: number;
}

const HORAS_RESOLUCION = `EXTRACT(EPOCH FROM (i.fecha_resolucion - COALESCE(i.fecha_asignacion, i.fecha_creacion))) / 3600.0`;
const TRUNC: Record<TendenciaDto['agrupacion'], string> = { dia: 'day', semana: 'week', mes: 'month' };

@Injectable()
export class ReportesService {
  private readonly tz: string;

  constructor(
    @InjectRepository(Incidencia) private readonly repo: Repository<Incidencia>,
    config: ConfigService,
  ) {
    this.tz = config.get<string>('TZ_REPORTES', 'America/Lima');
  }

  async resumen(r: RangoFechasDto) {
    const kpi = await this.base(r)
      .select('COUNT(*)::int', 'total')
      .addSelect(`COUNT(*) FILTER (WHERE i.estado = '${E.ABIERTA}')::int`, 'pendientes')
      .addSelect(`COUNT(*) FILTER (WHERE i.estado IN ('${E.ASIGNADA}', '${E.EN_PROGRESO}'))::int`, 'enCurso')
      .addSelect(`COUNT(*) FILTER (WHERE i.estado IN ('${E.RESUELTA}', '${E.CERRADA}'))::int`, 'resueltas')
      .addSelect(`COUNT(*) FILTER (WHERE i.estado = '${E.CANCELADA}')::int`, 'canceladas')
      .addSelect(
        `COUNT(*) FILTER (WHERE i.prioridad = 'CRITICA' AND i.estado NOT IN ('${E.RESUELTA}', '${E.CERRADA}', '${E.CANCELADA}'))::int`,
        'criticasAbiertas',
      )
      .addSelect(`ROUND(AVG(${HORAS_RESOLUCION})::numeric, 1)::float`, 'promedioHorasResolucion')
      .getRawOne();

    const [porEstado, porPrioridad, porCategoria] = await Promise.all([
      this.agrupar(r, 'i.estado'),
      this.agrupar(r, 'i.prioridad'),
      this.agrupar(r, 'c.nombre'),
    ]);

    return {
      ...kpi,
      porEstado: this.completar(porEstado, Object.values(E)),
      porPrioridad: this.completar(porPrioridad, ['CRITICA', 'ALTA', 'MEDIA', 'BAJA']),
      porCategoria,
    };
  }

  /** Incidencias creadas vs. resueltas por periodo, incluyendo periodos sin actividad. */
  async tendencia(t: TendenciaDto) {
    const unidad = TRUNC[t.agrupacion ?? 'dia'];
    // 'en-CA' formatea como YYYY-MM-DD; usamos la zona horaria de la institución, no UTC
    const hasta = t.hasta ?? new Intl.DateTimeFormat('en-CA', { timeZone: this.tz }).format(new Date());
    const desde = t.desde ?? this.restarDias(hasta, unidad === 'month' ? 365 : unidad === 'week' ? 84 : 30);

    const rows: { periodo: string; creadas: number; resueltas: number }[] = await this.repo.query(
      `
      WITH periodos AS (
        SELECT generate_series(date_trunc($1, $2::date), date_trunc($1, $3::date), ('1 ' || $1)::interval)::date AS periodo
      ),
      creadas AS (
        SELECT date_trunc($1, fecha_creacion AT TIME ZONE $4)::date AS periodo, COUNT(*)::int AS n
        FROM incidencias
        WHERE (fecha_creacion AT TIME ZONE $4)::date BETWEEN $2::date AND $3::date
        GROUP BY 1
      ),
      resueltas AS (
        SELECT date_trunc($1, fecha_resolucion AT TIME ZONE $4)::date AS periodo, COUNT(*)::int AS n
        FROM incidencias
        WHERE fecha_resolucion IS NOT NULL
          AND (fecha_resolucion AT TIME ZONE $4)::date BETWEEN $2::date AND $3::date
        GROUP BY 1
      )
      SELECT to_char(p.periodo, 'YYYY-MM-DD') AS periodo,
             COALESCE(c.n, 0) AS creadas,
             COALESCE(r.n, 0) AS resueltas
      FROM periodos p
      LEFT JOIN creadas c ON c.periodo = p.periodo
      LEFT JOIN resueltas r ON r.periodo = p.periodo
      ORDER BY p.periodo
      `,
      [unidad, desde, hasta, this.tz],
    );
    return { agrupacion: t.agrupacion ?? 'dia', desde, hasta, puntos: rows };
  }

  /** Desempeño por técnico. */
  async tecnicos(r: RangoFechasDto) {
    const qb = this.repo.manager
      .createQueryBuilder()
      .from('usuarios', 'u')
      .leftJoin('incidencias', 'i', 'i.tecnico_id = u.id' + this.condicionRango(r, 'AND'), this.paramsRango(r))
      .select(['u.id AS "tecnicoId"', 'u.nombre AS nombre', 'u.especialidad AS especialidad'])
      .addSelect('COUNT(i.id)::int', 'asignadas')
      .addSelect(`COUNT(i.id) FILTER (WHERE i.estado IN ('${E.ASIGNADA}', '${E.EN_PROGRESO}'))::int`, 'enCurso')
      .addSelect(`COUNT(i.id) FILTER (WHERE i.estado IN ('${E.RESUELTA}', '${E.CERRADA}'))::int`, 'resueltas')
      .addSelect(`ROUND(AVG(${HORAS_RESOLUCION})::numeric, 1)::float`, 'promedioHorasResolucion')
      .where('u.rol = :rol', { rol: Rol.TECNICO })
      .groupBy('u.id')
      .orderBy('resueltas', 'DESC')
      .addOrderBy('u.nombre', 'ASC');
    return qb.getRawMany();
  }

  /** Tiempo promedio de resolución (horas) por prioridad y por categoría. */
  async tiempos(r: RangoFechasDto) {
    const porPrioridad = await this.base(r)
      .select('i.prioridad', 'clave')
      .addSelect(`ROUND(AVG(${HORAS_RESOLUCION})::numeric, 1)::float`, 'promedioHoras')
      .addSelect('COUNT(i.fecha_resolucion)::int', 'resueltas')
      .andWhere('i.fecha_resolucion IS NOT NULL')
      .groupBy('i.prioridad')
      .orderBy(`CASE i.prioridad WHEN 'CRITICA' THEN 0 WHEN 'ALTA' THEN 1 WHEN 'MEDIA' THEN 2 ELSE 3 END`)
      .getRawMany();
    const porCategoria = await this.base(r)
      .select('c.nombre', 'clave')
      .addSelect(`ROUND(AVG(${HORAS_RESOLUCION})::numeric, 1)::float`, 'promedioHoras')
      .addSelect('COUNT(i.fecha_resolucion)::int', 'resueltas')
      .andWhere('i.fecha_resolucion IS NOT NULL')
      .groupBy('c.nombre')
      .orderBy('"promedioHoras"', 'DESC')
      .getRawMany();
    return { porPrioridad, porCategoria };
  }

  async exportCsv(f: ExportDto): Promise<string> {
    const qb = this.base(f)
      .leftJoin('i.reportadoPor', 'rp')
      .leftJoin('i.tecnico', 't')
      .select([
        'i.codigo AS codigo',
        'i.titulo AS titulo',
        'c.nombre AS categoria',
        'i.prioridad AS prioridad',
        'i.estado AS estado',
        'i.ubicacion AS ubicacion',
        'rp.nombre AS reportado_por',
        't.nombre AS tecnico',
        `to_char(i.fecha_creacion AT TIME ZONE :tz, 'YYYY-MM-DD HH24:MI') AS fecha_creacion`,
        `to_char(i.fecha_asignacion AT TIME ZONE :tz, 'YYYY-MM-DD HH24:MI') AS fecha_asignacion`,
        `to_char(i.fecha_resolucion AT TIME ZONE :tz, 'YYYY-MM-DD HH24:MI') AS fecha_resolucion`,
        `to_char(i.fecha_cierre AT TIME ZONE :tz, 'YYYY-MM-DD HH24:MI') AS fecha_cierre`,
        `ROUND((${HORAS_RESOLUCION})::numeric, 1) AS horas_resolucion`,
      ])
      .setParameter('tz', this.tz)
      .orderBy('i.fecha_creacion', 'DESC');
    if (f.estado) qb.andWhere('i.estado = :estado', { estado: f.estado });

    const rows: Record<string, unknown>[] = await qb.getRawMany();
    const headers = [
      'codigo', 'titulo', 'categoria', 'prioridad', 'estado', 'ubicacion', 'reportado_por', 'tecnico',
      'fecha_creacion', 'fecha_asignacion', 'fecha_resolucion', 'fecha_cierre', 'horas_resolucion',
    ];
    const esc = (v: unknown) => {
      const s = v === null || v === undefined ? '' : String(v);
      return /[",\n;]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
    };
    // BOM para que Excel detecte UTF-8 (tildes y ñ)
    return '﻿' + [headers.join(','), ...rows.map((r) => headers.map((h) => esc(r[h])).join(','))].join('\n');
  }

  // ---------- helpers ----------

  private base(r: RangoFechasDto): SelectQueryBuilder<Incidencia> {
    const qb = this.repo.createQueryBuilder('i').leftJoin('i.categoria', 'c');
    const cond = this.condicionRango(r, '');
    if (cond) qb.where(cond, this.paramsRango(r));
    return qb;
  }

  private condicionRango(r: RangoFechasDto, prefijo: 'AND' | ''): string {
    const partes: string[] = [];
    if (r.desde) partes.push(`(i.fecha_creacion AT TIME ZONE :tzr)::date >= :desde::date`);
    if (r.hasta) partes.push(`(i.fecha_creacion AT TIME ZONE :tzr)::date <= :hasta::date`);
    if (!partes.length) return '';
    return `${prefijo ? ' AND ' : ''}${partes.join(' AND ')}`;
  }

  private paramsRango(r: RangoFechasDto) {
    return { desde: r.desde, hasta: r.hasta, tzr: this.tz };
  }

  private async agrupar(r: RangoFechasDto, expr: string): Promise<Conteo[]> {
    return this.base(r)
      .select(expr, 'clave')
      .addSelect('COUNT(*)::int', 'total')
      .groupBy(expr)
      .orderBy('total', 'DESC')
      .getRawMany<Conteo>();
  }

  /** Garantiza que todas las claves aparezcan (con 0) y en un orden estable. */
  private completar(datos: Conteo[], claves: string[]): Conteo[] {
    const m = new Map(datos.map((d) => [d.clave, d.total]));
    return claves.map((clave) => ({ clave, total: m.get(clave) ?? 0 }));
  }

  private restarDias(fecha: string, dias: number): string {
    const d = new Date(`${fecha}T00:00:00Z`);
    d.setUTCDate(d.getUTCDate() - dias);
    return d.toISOString().slice(0, 10);
  }
}
