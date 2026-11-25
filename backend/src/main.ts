import { NestFactory } from '@nestjs/core';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { AppModule } from './app.module';
import { setupApp } from './setup-app';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  setupApp(app);

  const config = new DocumentBuilder()
    .setTitle('Sistema de Gestión de Incidencias')
    .setDescription('API REST para registrar, asignar y dar seguimiento a incidencias técnicas')
    .setVersion('1.0')
    .addBearerAuth()
    .build();
  SwaggerModule.setup('api/docs', app, SwaggerModule.createDocument(app, config));

  const port = Number(process.env.PORT ?? 3000);
  // 0.0.0.0 para que el emulador / dispositivo físico pueda conectarse
  await app.listen(port, '0.0.0.0');
  console.log(`API lista en http://localhost:${port}/api  ·  Docs: http://localhost:${port}/api/docs`);
}
bootstrap();
