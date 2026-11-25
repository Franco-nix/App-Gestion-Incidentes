import { Body, Controller, Get, Param, ParseIntPipe, Patch, Post, Query } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { Rol } from '../common/enums';
import { Roles } from '../auth/decorators/roles.decorator';
import { AuthUser, CurrentUser } from '../auth/decorators/current-user.decorator';
import { IncidenciasService } from './incidencias.service';
import {
  AsignarDto,
  CambiarEstadoDto,
  ComentarioDto,
  CreateIncidenciaDto,
  FiltroIncidenciasDto,
  UpdateIncidenciaDto,
} from './dto/incidencia.dto';

@ApiTags('incidencias')
@ApiBearerAuth()
@Controller('incidencias')
export class IncidenciasController {
  constructor(private readonly service: IncidenciasService) {}

  @Post()
  create(@Body() dto: CreateIncidenciaDto, @CurrentUser() user: AuthUser) {
    return this.service.create(dto, user);
  }

  @Get()
  findAll(@Query() filtro: FiltroIncidenciasDto, @CurrentUser() user: AuthUser) {
    return this.service.findAll(filtro, user);
  }

  @Get(':id')
  findOne(@Param('id', ParseIntPipe) id: number, @CurrentUser() user: AuthUser) {
    return this.service.findOne(id, user);
  }

  @Patch(':id')
  update(@Param('id', ParseIntPipe) id: number, @Body() dto: UpdateIncidenciaDto, @CurrentUser() user: AuthUser) {
    return this.service.update(id, dto, user);
  }

  @Roles(Rol.ADMIN)
  @Patch(':id/asignar')
  asignar(@Param('id', ParseIntPipe) id: number, @Body() dto: AsignarDto, @CurrentUser() user: AuthUser) {
    return this.service.asignar(id, dto, user);
  }

  @Patch(':id/estado')
  cambiarEstado(@Param('id', ParseIntPipe) id: number, @Body() dto: CambiarEstadoDto, @CurrentUser() user: AuthUser) {
    return this.service.cambiarEstado(id, dto, user);
  }

  @Post(':id/comentarios')
  comentar(@Param('id', ParseIntPipe) id: number, @Body() dto: ComentarioDto, @CurrentUser() user: AuthUser) {
    return this.service.comentar(id, dto.comentario, user);
  }
}
