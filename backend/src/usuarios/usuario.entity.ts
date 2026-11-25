import { Column, CreateDateColumn, Entity, PrimaryGeneratedColumn, Unique } from 'typeorm';
import { Rol } from '../common/enums';

@Entity('usuarios')
@Unique('uq_usuarios_email', ['email'])
export class Usuario {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ length: 120 })
  nombre: string;

  @Column({ length: 160 })
  email: string;

  @Column({ name: 'password_hash', select: false })
  passwordHash: string;

  @Column({ type: 'enum', enum: Rol, enumName: 'rol_usuario', default: Rol.USUARIO })
  rol: Rol;

  @Column({ type: 'varchar', length: 120, nullable: true })
  area: string | null;

  @Column({ type: 'varchar', length: 120, nullable: true })
  especialidad: string | null;

  @Column({ default: true })
  activo: boolean;

  @CreateDateColumn({ name: 'created_at', type: 'timestamptz' })
  createdAt: Date;
}
