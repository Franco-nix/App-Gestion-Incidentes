import { DataSourceOptions } from 'typeorm';
import { Usuario } from '../usuarios/usuario.entity';
import { Categoria } from '../categorias/categoria.entity';
import { Incidencia } from '../incidencias/incidencia.entity';
import { Seguimiento } from '../incidencias/seguimiento.entity';

export const ENTITIES = [Usuario, Categoria, Incidencia, Seguimiento];

/** Opciones compartidas por la app Nest y por el CLI de TypeORM (migraciones). */
export function buildDataSourceOptions(env: NodeJS.ProcessEnv = process.env): DataSourceOptions {
  return {
    type: 'postgres',
    host: env.DB_HOST ?? 'localhost',
    port: Number(env.DB_PORT ?? 5433),
    username: env.DB_USER ?? 'incidencias',
    password: env.DB_PASSWORD ?? 'incidencias',
    database: env.DB_NAME ?? 'incidencias',
    entities: ENTITIES,
    migrations: [__dirname + '/../database/migrations/*.{ts,js}'],
    synchronize: false,
  };
}
