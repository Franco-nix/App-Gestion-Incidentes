import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Incidencia } from './incidencia.entity';
import { Seguimiento } from './seguimiento.entity';
import { IncidenciasController } from './incidencias.controller';
import { IncidenciasService } from './incidencias.service';

@Module({
  imports: [TypeOrmModule.forFeature([Incidencia, Seguimiento])],
  controllers: [IncidenciasController],
  providers: [IncidenciasService],
})
export class IncidenciasModule {}
