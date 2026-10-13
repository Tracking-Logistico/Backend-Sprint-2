# Tracking Logístico — Frontend

Frontend React y Vite. Es un repositorio independiente del backend; se comunican mediante la API HTTP.

## Ejecución local

```bash
npm ci
npm run dev
```

Abre http://localhost:5173. Si no se define `VITE_API_URL`, la aplicación apunta a `http://localhost:8080/api/v1`. Puedes copiar `.env.example` a `.env` y cambiar la URL para tus pruebas privadas. Los archivos `.env`, `.env.local`, `.env.production` y demás variantes de entorno no se suben a GitHub. `.env.example` contiene únicamente localhost y sí se versiona.

## Docker con ambos repositorios independientes

Con ambas carpetas `tracking-logistico-backend-main` y `tracking-logistico-frontend-main` en el mismo directorio padre:

```bash
cd tracking-logistico-backend-main
docker compose up --build
```

Abre http://localhost:5173. Docker Compose envía `VITE_API_URL=http://localhost:8080/api/v1` al servidor de desarrollo y activa PostgreSQL solo en el backend Docker. Si ejecutas Java localmente, el backend usa H2; si ejecutas Docker Compose, usa PostgreSQL.

## Netlify

Configura `VITE_API_URL` en las variables de entorno del sitio Netlify con la URL **existente** del backend Render terminada en `/api/v1`. La URL de producción ya no está escrita en el código ni en archivos versionados: Vite incorpora la variable en el momento de `npm run build`, por lo que debes confirmar que esté definida antes de publicar una nueva versión. No copies tu `.env` local al repositorio ni al build de producción.

```bash
npm run build
npm run lint
```

El backend debe permitir el origen público del frontend mediante `CORS_ALLOWED_ORIGIN`. Los módulos conservan sus endpoints, permisos y pantallas existentes.
