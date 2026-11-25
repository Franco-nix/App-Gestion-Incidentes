import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Transform, Type } from 'class-transformer';
import {
  IsBoolean,
  IsEnum,
  IsInt,
  IsOptional,
  IsPositive,
  IsString,
  Max,
  MaxLength,
  Min,
  MinLength,
} from 'class-validator';
import { EstadoIncidencia, Prioridad } from '../../common/enums';

export class CreateIncidenciaDto {
  @ApiProperty() @IsString() @MinLength(5, { message: 'El título debe tener al menos 5 caracteres' }) @MaxLength(150)
  titulo: string;

  @ApiProperty() @IsString() @MinLength(10, { message: 'Describe la incidencia con al menos 10 caracteres' })
  descripcion: string;

  @ApiPropertyOptional() @IsOptional() @IsString() @MaxLength(150)
  ubicacion?: string;

  @ApiProperty() @Type(() => Number) @IsInt() @IsPositive()
  categoriaId: number;

  @ApiProperty({ enum: Prioridad }) @IsEnum(Prioridad)
  prioridad: Prioridad;
}

export class UpdateIncidenciaDto {
  @ApiPropertyOptional() @IsOptional() @IsString() @MinLength(5) @MaxLength(150)
  titulo?: string;

  @ApiPropertyOptional() @IsOptional() @IsString() @MinLength(10)
  descripcion?: string;

  @ApiPropertyOptional() @IsOptional() @IsString() @MaxLength(150)
  ubicacion?: string;

  @ApiPropertyOptional() @IsOptional() @Type(() => Number) @IsInt() @IsPositive()
  categoriaId?: number;

  @ApiPropertyOptional({ enum: Prioridad }) @IsOptional() @IsEnum(Prioridad)
  prioridad?: Prioridad;
}

export class AsignarDto {
  @ApiProperty() @Type(() => Number) @IsInt() @IsPositive()
  tecnicoId: number;

  @ApiPropertyOptional() @IsOptional() @IsString() @MaxLength(1000)
  comentario?: string;
}

export class CambiarEstadoDto {
  @ApiProperty({ enum: EstadoIncidencia }) @IsEnum(EstadoIncidencia)
  estado: EstadoIncidencia;

  @ApiPropertyOptional() @IsOptional() @IsString() @MaxLength(1000)
  comentario?: string;
}

export class ComentarioDto {
  @ApiProperty() @IsString() @MinLength(1) @MaxLength(1000)
  comentario: string;
}

export class FiltroIncidenciasDto {
  @ApiPropertyOptional({ enum: EstadoIncidencia }) @IsOptional() @IsEnum(EstadoIncidencia)
  estado?: EstadoIncidencia;

  @ApiPropertyOptional({ enum: Prioridad }) @IsOptional() @IsEnum(Prioridad)
  prioridad?: Prioridad;

  @ApiPropertyOptional() @IsOptional() @Type(() => Number) @IsInt()
  categoriaId?: number;

  @ApiPropertyOptional() @IsOptional() @Type(() => Number) @IsInt()
  tecnicoId?: number;

  @ApiPropertyOptional({ description: 'Solo incidencias sin técnico asignado' })
  @IsOptional()
  @Transform(({ value }) => value === true || value === 'true')
  @IsBoolean()
  sinAsignar?: boolean;

  @ApiPropertyOptional({ description: 'Búsqueda por código, título o descripción' })
  @IsOptional() @IsString() @MaxLength(100)
  q?: string;

  @ApiPropertyOptional({ default: 1 }) @IsOptional() @Type(() => Number) @IsInt() @Min(1)
  page: number = 1;

  @ApiPropertyOptional({ default: 20 }) @IsOptional() @Type(() => Number) @IsInt() @Min(1) @Max(100)
  size: number = 20;
}
