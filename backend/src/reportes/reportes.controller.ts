import { Controller, Get, Header, Query } from '@nestjs/common';
import { ApiBearerAuth, ApiProduces, ApiTags } from '@nestjs/swagger';
import { Rol } from '../common/enums';
import { Roles } from '../auth/decorators/roles.decorator';
import { ReportesService } from './reportes.service';
import { ExportDto, RangoFechasDto, TendenciaDto } from './dto/rango.dto';

@ApiTags('reportes')
@ApiBearerAuth()
@Roles(Rol.ADMIN)
@Controller('reportes')
export class ReportesController {
  constructor(private readonly service: ReportesService) {}

  @Get('resumen')
  resumen(@Query() r: RangoFechasDto) {
    return this.service.resumen(r);
  }

  @Get('tendencia')
  tendencia(@Query() t: TendenciaDto) {
    return this.service.tendencia(t);
  }

  @Get('tecnicos')
  tecnicos(@Query() r: RangoFechasDto) {
    return this.service.tecnicos(r);
  }

  @Get('tiempos')
  tiempos(@Query() r: RangoFechasDto) {
    return this.service.tiempos(r);
  }

  @Get('export.csv')
  @ApiProduces('text/csv')
  @Header('Content-Type', 'text/csv; charset=utf-8')
  @Header('Content-Disposition', 'attachment; filename="incidencias.csv"')
  exportCsv(@Query() f: ExportDto) {
    return this.service.exportCsv(f);
  }
}
