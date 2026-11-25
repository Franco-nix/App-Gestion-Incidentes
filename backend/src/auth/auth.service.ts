import { Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcryptjs';
import { UsuariosService } from '../usuarios/usuarios.service';
import { Usuario } from '../usuarios/usuario.entity';

@Injectable()
export class AuthService {
  constructor(
    private readonly usuarios: UsuariosService,
    private readonly jwt: JwtService,
  ) {}

  async login(email: string, password: string): Promise<{ accessToken: string; usuario: Usuario }> {
    const usuario = await this.usuarios.findByEmailWithPassword(email);
    if (!usuario || !(await bcrypt.compare(password, usuario.passwordHash))) {
      throw new UnauthorizedException('Credenciales incorrectas');
    }
    if (!usuario.activo) throw new UnauthorizedException('Usuario desactivado');

    const accessToken = await this.jwt.signAsync({ sub: usuario.id, rol: usuario.rol });
    const { passwordHash: _omit, ...publico } = usuario;
    return { accessToken, usuario: publico as Usuario };
  }
}
