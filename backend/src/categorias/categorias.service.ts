import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Categoria } from './categoria.entity';
import { CreateCategoriaDto, UpdateCategoriaDto } from './dto/categoria.dto';

@Injectable()
export class CategoriasService {
  constructor(@InjectRepository(Categoria) private readonly repo: Repository<Categoria>) {}

  findAll(soloActivas = true): Promise<Categoria[]> {
    return this.repo.find({ where: soloActivas ? { activo: true } : {}, order: { nombre: 'ASC' } });
  }

  async findOne(id: number): Promise<Categoria> {
    const c = await this.repo.findOneBy({ id });
    if (!c) throw new NotFoundException('Categoría no encontrada');
    return c;
  }

  create(dto: CreateCategoriaDto): Promise<Categoria> {
    return this.repo.save(this.repo.create({ nombre: dto.nombre.trim(), descripcion: dto.descripcion ?? null }));
  }

  async update(id: number, dto: UpdateCategoriaDto): Promise<Categoria> {
    const c = await this.findOne(id);
    Object.assign(c, dto);
    return this.repo.save(c);
  }
}
