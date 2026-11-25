import {
  Check,
  Column,
  CreateDateColumn,
  Entity,
  Index,
  JoinColumn,
  ManyToOne,
  OneToMany,
  PrimaryGeneratedColumn,
  Unique,
  UpdateDateColumn,
} from 'typeorm';
import { EstadoIncidencia, Prioridad } from '../common/enums';
import { Categoria } from '../categorias/categoria.entity';
import { Usuario } from '../usuarios/usuario.entity';
import { Seguimiento } from './seguimiento.entity';

@Entity('incidencias')
@Unique('uq_incidencias_codigo', ['codigo'])
@Check(
  'ck_incidencias_tecnico_requerido',
  `estado NOT IN ('ASIGNADA', 'EN_PROGRESO', 'RESUELTA') OR tecnico_id IS NOT NULL`,
)
export class Incidencia {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ length: 20 })
  codigo: string;

  @Column({ length: 150 })
  titulo: string;

  @Column({ type: 'text' })
  descripcion: string;

  @Column({ type: 'varchar', length: 150, nullable: true })
  ubicacion: string | null;

  @ManyToOne(() => Categoria, { nullable: false, eager: true })
  @JoinColumn({ name: 'categoria_id', foreignKeyConstraintName: 'fk_incidencias_categoria' })
  categoria: Categoria;

  @Column({ name: 'categoria_id' })
  categoriaId: number;

  @Column({ type: 'enum', enum: Prioridad, enumName: 'prioridad_incidencia', default: Prioridad.MEDIA })
  prioridad: Prioridad;

  @Index('ix_incidencias_estado')
  @Column({
    type: 'enum',
    enum: EstadoIncidencia,
    enumName: 'estado_incidencia',
    default: EstadoIncidencia.ABIERTA,
  })
  estado: EstadoIncidencia;

  @ManyToOne(() => Usuario, { nullable: false, eager: true })
  @JoinColumn({ name: 'reportado_por', foreignKeyConstraintName: 'fk_incidencias_reportante' })
  reportadoPor: Usuario;

  @Index('ix_incidencias_reportante')
  @Column({ name: 'reportado_por' })
  reportadoPorId: number;

  @ManyToOne(() => Usuario, { nullable: true, eager: true })
  @JoinColumn({ name: 'tecnico_id', foreignKeyConstraintName: 'fk_incidencias_tecnico' })
  tecnico: Usuario | null;

  @Index('ix_incidencias_tecnico')
  @Column({ name: 'tecnico_id', type: 'int', nullable: true })
  tecnicoId: number | null;

  @Index('ix_incidencias_fecha_creacion')
  @CreateDateColumn({ name: 'fecha_creacion', type: 'timestamptz' })
  fechaCreacion: Date;

  @Column({ name: 'fecha_asignacion', type: 'timestamptz', nullable: true })
  fechaAsignacion: Date | null;

  @Column({ name: 'fecha_resolucion', type: 'timestamptz', nullable: true })
  fechaResolucion: Date | null;

  @Column({ name: 'fecha_cierre', type: 'timestamptz', nullable: true })
  fechaCierre: Date | null;

  @UpdateDateColumn({ name: 'updated_at', type: 'timestamptz' })
  updatedAt: Date;

  @OneToMany(() => Seguimiento, (s) => s.incidencia)
  seguimientos?: Seguimiento[];
}
