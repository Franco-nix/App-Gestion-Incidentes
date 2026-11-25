import { Column, CreateDateColumn, Entity, Index, JoinColumn, ManyToOne, PrimaryGeneratedColumn } from 'typeorm';
import { EstadoIncidencia, TipoSeguimiento } from '../common/enums';
import { Usuario } from '../usuarios/usuario.entity';
import { Incidencia } from './incidencia.entity';

@Entity('seguimientos')
@Index('ix_seguimientos_incidencia', ['incidenciaId', 'createdAt'])
export class Seguimiento {
  @PrimaryGeneratedColumn()
  id: number;

  @ManyToOne(() => Incidencia, (i) => i.seguimientos, { nullable: false, onDelete: 'CASCADE' })
  @JoinColumn({ name: 'incidencia_id', foreignKeyConstraintName: 'fk_seguimientos_incidencia' })
  incidencia: Incidencia;

  @Column({ name: 'incidencia_id' })
  incidenciaId: number;

  @ManyToOne(() => Usuario, { nullable: false, eager: true })
  @JoinColumn({ name: 'usuario_id', foreignKeyConstraintName: 'fk_seguimientos_usuario' })
  usuario: Usuario;

  @Column({ name: 'usuario_id' })
  usuarioId: number;

  @Column({ type: 'enum', enum: TipoSeguimiento, enumName: 'tipo_seguimiento' })
  tipo: TipoSeguimiento;

  @Column({ name: 'estado_anterior', type: 'enum', enum: EstadoIncidencia, enumName: 'estado_incidencia', nullable: true })
  estadoAnterior: EstadoIncidencia | null;

  @Column({ name: 'estado_nuevo', type: 'enum', enum: EstadoIncidencia, enumName: 'estado_incidencia', nullable: true })
  estadoNuevo: EstadoIncidencia | null;

  @Column({ type: 'text', nullable: true })
  comentario: string | null;

  @CreateDateColumn({ name: 'created_at', type: 'timestamptz' })
  createdAt: Date;
}
