import { Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { UsuariosService } from '../usuarios/usuarios.service';
import { AuthUser } from './decorators/current-user.decorator';

interface JwtPayload {
  sub: number;
}

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  constructor(
    config: ConfigService,
    private readonly usuarios: UsuariosService,
  ) {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey: config.getOrThrow<string>('JWT_SECRET'),
    });
  }

  /** Se consulta la BD en cada request para que un usuario desactivado pierda acceso de inmediato. */
  async validate(payload: JwtPayload): Promise<AuthUser> {
    const u = await this.usuarios.findById(payload.sub);
    if (!u || !u.activo) throw new UnauthorizedException('Sesión inválida');
    return { id: u.id, email: u.email, nombre: u.nombre, rol: u.rol };
  }
}
