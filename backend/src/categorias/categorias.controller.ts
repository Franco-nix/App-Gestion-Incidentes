import { Body, Controller, Get, Param, ParseBoolPipe, ParseIntPipe, Patch, Post, Query } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { Rol } from '../common/enums';
import { Roles } from '../auth/decorators/roles.decorator';
import { CategoriasService } from './categorias.service';
import { CreateCategoriaDto, UpdateCategoriaDto } from './dto/categoria.dto';

@ApiTags('categorias')
@ApiBearerAuth()
@Controller('categorias')
export class CategoriasController {
  constructor(private readonly service: CategoriasService) {}

  @Get()
  findAll(@Query('todas', new ParseBoolPipe({ optional: true })) todas?: boolean) {
    return this.service.findAll(!todas);
  }

  @Roles(Rol.ADMIN)
  @Post()
  create(@Body() dto: CreateCategoriaDto) {
    return this.service.create(dto);
  }

  @Roles(Rol.ADMIN)
  @Patch(':id')
  update(@Param('id', ParseIntPipe) id: number, @Body() dto: UpdateCategoriaDto) {
    return this.service.update(id, dto);
  }
}
