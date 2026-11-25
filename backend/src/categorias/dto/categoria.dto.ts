import { ApiProperty, ApiPropertyOptional, PartialType } from '@nestjs/swagger';
import { IsBoolean, IsOptional, IsString, MaxLength, MinLength } from 'class-validator';

export class CreateCategoriaDto {
  @ApiProperty() @IsString() @MinLength(2) @MaxLength(80)
  nombre: string;

  @ApiPropertyOptional() @IsOptional() @IsString() @MaxLength(255)
  descripcion?: string;
}

export class UpdateCategoriaDto extends PartialType(CreateCategoriaDto) {
  @ApiPropertyOptional() @IsOptional() @IsBoolean()
  activo?: boolean;
}
