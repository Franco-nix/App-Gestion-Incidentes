import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { APP_FILTER, APP_GUARD } from '@nestjs/core';
import { TypeOrmModule } from '@nestjs/typeorm';
import { buildDataSourceOptions } from './config/database.config';
import { AuthModule } from './auth/auth.module';
import { JwtAuthGuard } from './auth/guards/jwt-auth.guard';
import { RolesGuard } from './auth/guards/roles.guard';
import { UsuariosModule } from './usuarios/usuarios.module';
import { CategoriasModule } from './categorias/categorias.module';
import { IncidenciasModule } from './incidencias/incidencias.module';
import { ReportesModule } from './reportes/reportes.module';
import { DbExceptionFilter } from './common/filters/db-exception.filter';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    TypeOrmModule.forRootAsync({ useFactory: () => buildDataSourceOptions(process.env) }),
    AuthModule,
    UsuariosModule,
    CategoriasModule,
    IncidenciasModule,
    ReportesModule,
  ],
  providers: [
    // Orden importante: primero autenticación (JWT), luego autorización (roles)
    { provide: APP_GUARD, useClass: JwtAuthGuard },
    { provide: APP_GUARD, useClass: RolesGuard },
    { provide: APP_FILTER, useClass: DbExceptionFilter },
  ],
})
export class AppModule {}
