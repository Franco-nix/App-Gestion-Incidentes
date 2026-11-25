/**
 * Pruebas e2e contra el PostgreSQL de docker-compose.
 * Requiere: docker compose up -d && npm run migration:run && npm run seed
 */
import 'dotenv/config';
import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { AppModule } from '../src/app.module';
import { setupApp } from '../src/setup-app';

describe('Flujo de incidencias (e2e)', () => {
  let app: INestApplication;
  const tokens: Record<string, string> = {};

  const login = async (email: string, password: string) => {
    const res = await request(app.getHttpServer()).post('/api/auth/login').send({ email, password }).expect(200);
    return res.body.accessToken as string;
  };
  const api = (rol: string) => ({
    get: (url: string) => request(app.getHttpServer()).get(url).set('Authorization', `Bearer ${tokens[rol]}`),
    post: (url: string) => request(app.getHttpServer()).post(url).set('Authorization', `Bearer ${tokens[rol]}`),
    patch: (url: string) => request(app.getHttpServer()).patch(url).set('Authorization', `Bearer ${tokens[rol]}`),
  });

  beforeAll(async () => {
    const mod = await Test.createTestingModule({ imports: [AppModule] }).compile();
    app = mod.createNestApplication();
    setupApp(app);
    await app.init();
    tokens.admin = await login('admin@inst.edu', 'Admin123*');
    tokens.tecnico = await login('carlos.ruiz@inst.edu', 'Tecnico123*');
    tokens.usuario = await login('maria.lopez@inst.edu', 'Usuario123*');
    tokens.otro = await login('pedro.quispe@inst.edu', 'Usuario123*');
  });

  afterAll(() => app.close());

  it('rechaza credenciales inválidas y peticiones sin token', async () => {
    await request(app.getHttpServer())
      .post('/api/auth/login')
      .send({ email: 'admin@inst.edu', password: 'incorrecta' })
      .expect(401);
    await request(app.getHttpServer()).get('/api/incidencias').expect(401);
  });

  it('GET /auth/me devuelve el usuario sin hash de contraseña', async () => {
    const res = await api('tecnico').get('/api/auth/me').expect(200);
    expect(res.body).toMatchObject({ email: 'carlos.ruiz@inst.edu', rol: 'TECNICO' });
    expect(res.body.passwordHash).toBeUndefined();
  });

  it('ciclo completo: registrar → asignar → en progreso → resolver → cerrar', async () => {
    const cats = await api('usuario').get('/api/categorias').expect(200);
    const creada = await api('usuario')
      .post('/api/incidencias')
      .send({
        titulo: 'Prueba e2e de incidencia',
        descripcion: 'Descripción generada por la prueba e2e',
        categoriaId: cats.body[0].id,
        prioridad: 'MEDIA',
      })
      .expect(201);
    const id = creada.body.id;
    expect(creada.body.codigo).toMatch(/^INC-\d{4}-\d{5}$/);
    expect(creada.body.estado).toBe('ABIERTA');

    // Otro usuario no la ve; el usuario no puede asignar
    await api('otro').get(`/api/incidencias/${id}`).expect(403);
    await api('usuario').patch(`/api/incidencias/${id}/asignar`).send({ tecnicoId: 2 }).expect(403);

    const tecnicos = await api('admin').get('/api/usuarios/tecnicos').expect(200);
    const carlos = tecnicos.body.find((t: { email: string }) => t.email === 'carlos.ruiz@inst.edu');
    const asignada = await api('admin').patch(`/api/incidencias/${id}/asignar`).send({ tecnicoId: carlos.id }).expect(200);
    expect(asignada.body.estado).toBe('ASIGNADA');

    const enCurso = await api('tecnico').patch(`/api/incidencias/${id}/estado`).send({ estado: 'EN_PROGRESO' }).expect(200);
    expect(enCurso.body.acciones.transiciones).toEqual(['RESUELTA']);

    await api('tecnico').patch(`/api/incidencias/${id}/estado`).send({ estado: 'RESUELTA' }).expect(400);
    await api('tecnico')
      .patch(`/api/incidencias/${id}/estado`)
      .send({ estado: 'RESUELTA', comentario: 'Solucionado en la prueba' })
      .expect(200);

    await api('usuario').post(`/api/incidencias/${id}/comentarios`).send({ comentario: 'Conforme' }).expect(201);
    const cerrada = await api('usuario').patch(`/api/incidencias/${id}/estado`).send({ estado: 'CERRADA' }).expect(200);
    expect(cerrada.body.estado).toBe('CERRADA');
    expect(cerrada.body.fechaCierre).toBeTruthy();
    expect(cerrada.body.seguimientos.map((s: { tipo: string }) => s.tipo)).toEqual([
      'CREACION',
      'ASIGNACION',
      'CAMBIO_ESTADO',
      'CAMBIO_ESTADO',
      'COMENTARIO',
      'CAMBIO_ESTADO',
    ]);

    // Ya cerrada: no se permiten más cambios
    await api('admin').patch(`/api/incidencias/${id}/estado`).send({ estado: 'CANCELADA' }).expect(400);
  });

  it('cada rol solo lista lo que le corresponde', async () => {
    const tec = await api('tecnico').get('/api/incidencias?size=100').expect(200);
    expect(tec.body.items.every((i: { tecnicoId: number }) => i.tecnicoId === 2)).toBe(true);
    const usr = await api('usuario').get('/api/incidencias?size=100').expect(200);
    expect(usr.body.items.every((i: { reportadoPor: { email: string } }) => i.reportadoPor.email === 'maria.lopez@inst.edu')).toBe(true);
  });

  it('el admin no puede desactivarse a sí mismo', async () => {
    const me = await api('admin').get('/api/auth/me').expect(200);
    await api('admin').patch(`/api/usuarios/${me.body.id}`).send({ activo: false }).expect(400);
  });

  it('reportes: solo admin y con totales coherentes', async () => {
    await api('tecnico').get('/api/reportes/resumen').expect(403);
    const r = await api('admin').get('/api/reportes/resumen').expect(200);
    const sumaEstados = r.body.porEstado.reduce((a: number, e: { total: number }) => a + e.total, 0);
    expect(sumaEstados).toBe(r.body.total);
    await api('admin').get('/api/reportes/tendencia?agrupacion=semana').expect(200);
    await api('admin').get('/api/reportes/tecnicos').expect(200);
    await api('admin').get('/api/reportes/resumen?desde=no-es-fecha').expect(400);
  });
});
