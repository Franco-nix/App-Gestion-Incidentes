import { Body, Controller, Get, Param, ParseIntPipe, Patch, Post, Query } from '@nestjs/common';
import { ApiBearerAuth, ApiQuery, ApiTags } from '@nestjs/swagger';
import { Rol } from '../common/enums';
import { Roles } from '../auth/decorators/roles.decorator';
import { AuthUser, CurrentUser } from '../auth/decorators/current-user.decorator';
import { UsuariosService } from './usuarios.service';
import { CreateUsuarioDto, UpdateUsuarioDto } from './dto/usuario.dto';

@ApiTags('usuarios')
@ApiBearerAuth()
@Roles(Rol.ADMIN)
@Controller('usuarios')
export class UsuariosController {
  constructor(private readonly service: UsuariosService) {}

  @Get()
  @ApiQuery({ name: 'rol', enum: Rol, required: false })
  findAll(@Query('rol') rol?: Rol) {
    return this.service.findAll(rol);
  }

  @Get('tecnicos')
  tecnicos() {
    return this.service.tecnicosConCarga();
  }

  @Get(':id')
  findOne(@Param('id', ParseIntPipe) id: number) {
    return this.service.findOne(id);
  }

  @Post()
  create(@Body() dto: CreateUsuarioDto) {
    return this.service.create(dto);
  }

  @Patch(':id')
  update(@Param('id', ParseIntPipe) id: number, @Body() dto: UpdateUsuarioDto, @CurrentUser() user: AuthUser) {
    return this.service.update(id, dto, user.id);
  }
}
