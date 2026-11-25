# Sistema de Gestión de Incidencias

App Android (Jetpack Compose) + API REST (NestJS) + PostgreSQL para **registrar, asignar y dar seguimiento** a incidencias técnicas en una institución, con **reportes y estadísticas**.

```
┌──────────────┐   HTTP/JSON + JWT   ┌──────────────┐   TypeORM   ┌──────────────┐
│ App Android  │ ──────────────────▶ │ API NestJS   │ ──────────▶ │ PostgreSQL   │
│ (Compose)    │ ◀────────────────── │ :3000/api    │             │ (Docker)     │
└──────────────┘                     └──────────────┘             └──────────────┘
```

La app **nunca** se conecta directamente a la base de datos: toda regla de negocio y permiso se valida en la API.

## Módulos

| Módulo | Qué hace | Quién |
|---|---|---|
| **Registro** | Crear incidencias (título, descripción, categoría, prioridad, ubicación). Código automático `INC-2026-00042`. Editable mientras está *Abierta*. | Todos |
| **Asignación** | Bandeja de incidencias sin técnico; asignar o reasignar viendo la carga actual de cada técnico. | Admin |
| **Seguimiento** | Flujo de estados, comentarios e historial completo (quién, qué y cuándo). | Según rol |
| **Reportes** | KPIs, creadas vs. resueltas, distribución por estado, categoría y prioridad, tiempos de resolución, desempeño por técnico y exportación CSV (API). | Admin |
| **Usuarios** | Alta, edición, activación y desactivación de usuarios y roles. | Admin |

### Flujo de estados

```
ABIERTA ──asignar──▶ ASIGNADA ──técnico──▶ EN_PROGRESO ──técnico──▶ RESUELTA ──reportante──▶ CERRADA
                                                ▲                        │
                                                └──────── reabrir ───────┘
Cualquier estado no final ──admin──▶ CANCELADA
```

Las reglas viven en un único archivo, `backend/src/incidencias/estado.rules.ts`. La API devuelve en cada incidencia las `acciones` que el usuario actual puede realizar, y la app solo muestra esos botones.

## Requisitos

- Docker y Docker Compose
- Node.js 20 o superior (probado con 24)
- Android Studio (compileSdk 37, minSdk 36)

## Puesta en marcha

### 1. Base de datos y API

```bash
cd backend
cp .env.example .env          # ajusta si lo necesitas
docker compose up -d          # PostgreSQL 17 en el puerto 5433
npm install
npm run migration:run         # crea tablas, enums, índices y restricciones
npm run seed                  # datos de demostración (¡borra los datos existentes!)
npm run start:dev             # http://localhost:3000/api
```

- Documentación interactiva (Swagger): <http://localhost:3000/api/docs>
- pgAdmin (opcional): `docker compose --profile tools up -d`, luego abre <http://localhost:5050>

> El puerto **5433** se eligió porque el 5432 suele estar ocupado por otro PostgreSQL. Puedes cambiarlo en `.env`.

### 2. App Android

Abre la carpeta raíz en Android Studio y ejecuta la app en un emulador. Por defecto apunta a `http://10.0.2.2:3000/api/`, que es el *localhost* de tu PC visto desde el emulador.

**Dispositivo físico:** el teléfono debe estar en la misma red que la PC.

```bash
./gradlew installDebug -PapiUrl=http://192.168.1.50:3000/api/
```

Agrega también esa IP en `app/src/main/res/xml/network_security_config.xml`.

## Credenciales de demostración

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@inst.edu` | `Admin123*` |
| Técnico | `carlos.ruiz@inst.edu` (también `lucia.mendoza@`, `jorge.paredes@`) | `Tecnico123*` |
| Usuario | `maria.lopez@inst.edu` (también `pedro.quispe@`, `ana.torres@`) | `Usuario123*` |

En compilaciones *debug*, la pantalla de login tiene botones para rellenar estas cuentas.

## Pruebas

```bash
# Backend
cd backend
npm test            # unitarias: reglas de estado y permisos (26)
npm run test:e2e    # e2e contra PostgreSQL: login, flujo completo, permisos, reportes

# Android
./gradlew testDebugUnitTest   # navegación por rol, acciones, formatos
```

## Estructura

```
backend/
├── docker-compose.yml
├── src/
│   ├── auth/            # login JWT, guards global de autenticación y de roles
│   ├── usuarios/        # CRUD de usuarios y técnicos con su carga
│   ├── categorias/
│   ├── incidencias/     # registro, asignación, seguimiento y estado.rules.ts
│   ├── reportes/        # agregados SQL y exportación CSV
│   ├── database/        # migraciones + seed
│   └── common/          # enums y filtro de errores de PostgreSQL
└── test/                # e2e

app/src/main/java/com/ejemplo/gestion_incidencias/
├── data/
│   ├── remote/          # Retrofit, DTOs, interceptor JWT
│   ├── session/         # DataStore (token + usuario)
│   └── repository/      # repositorios con mensajes de error en español
├── domain/              # enums y textos de acciones
└── ui/
    ├── navigation/      # rutas tipadas y pestañas por rol
    ├── common/          # chips, gráficos (Canvas), formatos
    └── screens/         # login, incidencias, asignación, reportes, usuarios, perfil
```

## Endpoints principales

| Método | Ruta | Rol |
|---|---|---|
| POST | `/api/auth/login` | público |
| GET | `/api/auth/me` | autenticado |
| GET, POST | `/api/incidencias` | autenticado (el listado depende del rol) |
| GET, PATCH | `/api/incidencias/:id` | participante o admin |
| PATCH | `/api/incidencias/:id/asignar` | admin |
| PATCH | `/api/incidencias/:id/estado` | según `estado.rules.ts` |
| POST | `/api/incidencias/:id/comentarios` | participante o admin |
| GET | `/api/usuarios`, `/api/usuarios/tecnicos` | admin |
| GET | `/api/reportes/{resumen,tendencia,tecnicos,tiempos,export.csv}` | admin |
| GET | `/api/categorias` | autenticado |

## Notas de producción

- Cambia `JWT_SECRET` en `.env`.
- Sirve la API por HTTPS y elimina `network_security_config.xml`, o quita de él los dominios locales.
- `npm run seed` borra todos los datos; úsalo solo en desarrollo.
