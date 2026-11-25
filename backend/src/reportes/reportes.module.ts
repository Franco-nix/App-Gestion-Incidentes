import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Incidencia } from '../incidencias/incidencia.entity';
import { ReportesController } from './reportes.controller';
import { ReportesService } from './reportes.service';

@Module({
  imports: [TypeOrmModule.forFeature([Incidencia])],
  controllers: [ReportesController],
  providers: [ReportesService],
})
export class ReportesModule {}
