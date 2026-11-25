import { INestApplication, ValidationPipe } from '@nestjs/common';

/** Configuración común a main.ts y a las pruebas e2e. */
export function setupApp(app: INestApplication) {
  app.setGlobalPrefix('api');
  app.enableCors();
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
    }),
  );
}
