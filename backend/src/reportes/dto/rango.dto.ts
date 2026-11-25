import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsDateString, IsEnum, IsIn, IsOptional } from 'class-validator';
import { EstadoIncidencia } from '../../common/enums';

export class RangoFechasDto {
  @ApiPropertyOptional({ example: '2026-01-01', description: 'Fecha inicial (inclusive), sobre fecha de creación' })
  @IsOptional()
  @IsDateString({}, { message: 'desde debe tener formato YYYY-MM-DD' })
  desde?: string;

  @ApiPropertyOptional({ example: '2026-12-31', description: 'Fecha final (inclusive)' })
  @IsOptional()
  @IsDateString({}, { message: 'hasta debe tener formato YYYY-MM-DD' })
  hasta?: string;
}

export class TendenciaDto extends RangoFechasDto {
  @ApiPropertyOptional({ enum: ['dia', 'semana', 'mes'], default: 'dia' })
  @IsOptional()
  @IsIn(['dia', 'semana', 'mes'])
  agrupacion: 'dia' | 'semana' | 'mes' = 'dia';
}

export class ExportDto extends RangoFechasDto {
  @ApiPropertyOptional({ enum: EstadoIncidencia })
  @IsOptional()
  @IsEnum(EstadoIncidencia)
  estado?: EstadoIncidencia;
}
