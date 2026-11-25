import { Column, Entity, PrimaryGeneratedColumn, Unique } from 'typeorm';

@Entity('categorias')
@Unique('uq_categorias_nombre', ['nombre'])
export class Categoria {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ length: 80 })
  nombre: string;

  @Column({ type: 'varchar', length: 255, nullable: true })
  descripcion: string | null;

  @Column({ default: true })
  activo: boolean;
}
