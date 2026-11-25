import { MigrationInterface, QueryRunner } from 'typeorm';

export class InitialSchema1795488965577 implements MigrationInterface {
  name = 'InitialSchema1795488965577';

  public async up(q: QueryRunner): Promise<void> {
    // ----- Tipos enumerados -----
    await q.query(`CREATE TYPE rol_usuario AS ENUM ('ADMIN', 'TECNICO', 'USUARIO')`);
    await q.query(`CREATE TYPE prioridad_incidencia AS ENUM ('BAJA', 'MEDIA', 'ALTA', 'CRITICA')`);
    await q.query(
      `CREATE TYPE estado_incidencia AS ENUM ('ABIERTA', 'ASIGNADA', 'EN_PROGRESO', 'RESUELTA', 'CERRADA', 'CANCELADA')`,
    );
    await q.query(`CREATE TYPE tipo_seguimiento AS ENUM ('CREACION', 'COMENTARIO', 'CAMBIO_ESTADO', 'ASIGNACION')`);

    // ----- usuarios -----
    await q.query(`
      CREATE TABLE usuarios (
        id            SERIAL PRIMARY KEY,
        nombre        VARCHAR(120) NOT NULL,
        email         VARCHAR(160) NOT NULL CONSTRAINT uq_usuarios_email UNIQUE,
        password_hash VARCHAR      NOT NULL,
        rol           rol_usuario  NOT NULL DEFAULT 'USUARIO',
        area          VARCHAR(120),
        especialidad  VARCHAR(120),
        activo        BOOLEAN      NOT NULL DEFAULT TRUE,
        created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
      )`);

    // ----- categorias -----
    await q.query(`
      CREATE TABLE categorias (
        id          SERIAL PRIMARY KEY,
        nombre      VARCHAR(80)  NOT NULL CONSTRAINT uq_categorias_nombre UNIQUE,
        descripcion VARCHAR(255),
        activo      BOOLEAN      NOT NULL DEFAULT TRUE
      )`);

    // ----- incidencias -----
    await q.query(`
      CREATE TABLE incidencias (
        id               SERIAL PRIMARY KEY,
        codigo           VARCHAR(20)          NOT NULL CONSTRAINT uq_incidencias_codigo UNIQUE,
        titulo           VARCHAR(150)         NOT NULL,
        descripcion      TEXT                 NOT NULL,
        ubicacion        VARCHAR(150),
        categoria_id     INTEGER              NOT NULL CONSTRAINT fk_incidencias_categoria REFERENCES categorias(id),
        prioridad        prioridad_incidencia NOT NULL DEFAULT 'MEDIA',
        estado           estado_incidencia    NOT NULL DEFAULT 'ABIERTA',
        reportado_por    INTEGER              NOT NULL CONSTRAINT fk_incidencias_reportante REFERENCES usuarios(id),
        tecnico_id       INTEGER                       CONSTRAINT fk_incidencias_tecnico REFERENCES usuarios(id),
        fecha_creacion   TIMESTAMPTZ          NOT NULL DEFAULT now(),
        fecha_asignacion TIMESTAMPTZ,
        fecha_resolucion TIMESTAMPTZ,
        fecha_cierre     TIMESTAMPTZ,
        updated_at       TIMESTAMPTZ          NOT NULL DEFAULT now(),
        -- Una incidencia en curso siempre debe tener técnico
        CONSTRAINT ck_incidencias_tecnico_requerido
          CHECK (estado NOT IN ('ASIGNADA', 'EN_PROGRESO', 'RESUELTA') OR tecnico_id IS NOT NULL)
      )`);
    await q.query(`CREATE INDEX ix_incidencias_estado ON incidencias (estado)`);
    await q.query(`CREATE INDEX ix_incidencias_tecnico ON incidencias (tecnico_id)`);
    await q.query(`CREATE INDEX ix_incidencias_reportante ON incidencias (reportado_por)`);
    await q.query(`CREATE INDEX ix_incidencias_fecha_creacion ON incidencias (fecha_creacion)`);

    // ----- seguimientos (historial) -----
    await q.query(`
      CREATE TABLE seguimientos (
        id              SERIAL PRIMARY KEY,
        incidencia_id   INTEGER           NOT NULL CONSTRAINT fk_seguimientos_incidencia REFERENCES incidencias(id) ON DELETE CASCADE,
        usuario_id      INTEGER           NOT NULL CONSTRAINT fk_seguimientos_usuario REFERENCES usuarios(id),
        tipo            tipo_seguimiento  NOT NULL,
        estado_anterior estado_incidencia,
        estado_nuevo    estado_incidencia,
        comentario      TEXT,
        created_at      TIMESTAMPTZ       NOT NULL DEFAULT now()
      )`);
    await q.query(`CREATE INDEX ix_seguimientos_incidencia ON seguimientos (incidencia_id, created_at)`);
  }

  public async down(q: QueryRunner): Promise<void> {
    await q.query(`DROP TABLE seguimientos`);
    await q.query(`DROP TABLE incidencias`);
    await q.query(`DROP TABLE categorias`);
    await q.query(`DROP TABLE usuarios`);
    await q.query(`DROP TYPE tipo_seguimiento`);
    await q.query(`DROP TYPE estado_incidencia`);
    await q.query(`DROP TYPE prioridad_incidencia`);
    await q.query(`DROP TYPE rol_usuario`);
  }
}
