import { ApiProperty, ApiPropertyOptional, PartialType } from '@nestjs/swagger';
import { IsBoolean, IsEmail, IsEnum, IsOptional, IsString, MaxLength, MinLength } from 'class-validator';
import { Rol } from '../../common/enums';

export class CreateUsuarioDto {
  @ApiProperty() @IsString() @MinLength(2) @MaxLength(120)
  nombre: string;

  @ApiProperty() @IsEmail({}, { message: 'Email inválido' })
  email: string;

  @ApiProperty({ minLength: 6 }) @IsString() @MinLength(6, { message: 'La contraseña debe tener al menos 6 caracteres' })
  password: string;

  @ApiProperty({ enum: Rol }) @IsEnum(Rol)
  rol: Rol;

  @ApiPropertyOptional() @IsOptional() @IsString() @MaxLength(120)
  area?: string;

  @ApiPropertyOptional() @IsOptional() @IsString() @MaxLength(120)
  especialidad?: string;
}

export class UpdateUsuarioDto extends PartialType(CreateUsuarioDto) {
  @ApiPropertyOptional() @IsOptional() @IsBoolean()
  activo?: boolean;
}
