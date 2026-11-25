import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import * as bcrypt from 'bcryptjs';
import { Repository } from 'typeorm';
import { EstadoIncidencia, Rol } from '../common/enums';
import { Usuario } from './usuario.entity';
import { CreateUsuarioDto, UpdateUsuarioDto } from './dto/usuario.dto';

export interface TecnicoConCarga {
  id: number;
  nombre: string;
  email: string;
  especialidad: string | null;
  carga: number;
}

@Injectable()
export class UsuariosService {
  constructor(@InjectRepository(Usuario) private readonly repo: Repository<Usuario>) {}

  findAll(rol?: Rol): Promise<Usuario[]> {
    return this.repo.find({ where: rol ? { rol } : {}, order: { nombre: 'ASC' } });
  }

  findById(id: number): Promise<Usuario | null> {
    return this.repo.findOneBy({ id });
  }

  async findOne(id: number): Promise<Usuario> {
    const u = await this.findById(id);
    if (!u) throw new NotFoundException('Usuario no encontrado');
    return u;
  }

  findByEmailWithPassword(email: string): Promise<Usuario | null> {
    return this.repo
      .createQueryBuilder('u')
      .addSelect('u.passwordHash')
      .where('LOWER(u.email) = LOWER(:email)', { email })
      .getOne();
  }

  async create(dto: CreateUsuarioDto): Promise<Usuario> {
    const entity = this.repo.create({
      nombre: dto.nombre.trim(),
      email: dto.email.toLowerCase().trim(),
      passwordHash: await bcrypt.hash(dto.password, 10),
      rol: dto.rol,
      area: dto.area ?? null,
      especialidad: dto.especialidad ?? null,
    });
    const saved = await this.repo.save(entity);
    return this.findOne(saved.id);
  }

  async update(id: number, dto: UpdateUsuarioDto, actorId?: number): Promise<Usuario> {
    const u = await this.findOne(id);
    // Evita que un administrador se bloquee a sí mismo fuera del sistema
    if (actorId === id && (dto.activo === false || (dto.rol && dto.rol !== u.rol))) {
      throw new BadRequestException('No puedes desactivar tu propia cuenta ni cambiar tu propio rol');
    }
    const { password, email, ...resto } = dto;
    Object.assign(u, resto);
    if (email) u.email = email.toLowerCase().trim();
    if (password) u.passwordHash = await bcrypt.hash(password, 10);
    await this.repo.save(u);
    return this.findOne(id);
  }

  /** Técnicos activos con el número de incidencias en curso (ASIGNADA / EN_PROGRESO). */
  async tecnicosConCarga(): Promise<TecnicoConCarga[]> {
    const rows = await this.repo
      .createQueryBuilder('u')
      .leftJoin('incidencias', 'i', 'i.tecnico_id = u.id AND i.estado IN (:...activos)', {
        activos: [EstadoIncidencia.ASIGNADA, EstadoIncidencia.EN_PROGRESO],
      })
      .select(['u.id AS id', 'u.nombre AS nombre', 'u.email AS email', 'u.especialidad AS especialidad'])
      .addSelect('COUNT(i.id)::int', 'carga')
      .where('u.rol = :rol AND u.activo = true', { rol: Rol.TECNICO })
      .groupBy('u.id')
      .orderBy('carga', 'ASC')
      .addOrderBy('u.nombre', 'ASC')
      .getRawMany<TecnicoConCarga>();
    return rows;
  }
}
