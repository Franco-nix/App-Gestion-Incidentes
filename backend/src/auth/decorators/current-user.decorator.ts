import { createParamDecorator, ExecutionContext } from '@nestjs/common';
import { Rol } from '../../common/enums';

/** Usuario autenticado extraído del JWT. */
export interface AuthUser {
  id: number;
  email: string;
  nombre: string;
  rol: Rol;
}

export const CurrentUser = createParamDecorator(
  (_data: unknown, ctx: ExecutionContext): AuthUser => ctx.switchToHttp().getRequest().user,
);
