# Docker Desktop para macOS — Tracking Logístico

Corrección para equipos Apple Silicon (M1/M2/M3/M4/M5) y Mac Intel. **Docker ejecuta contenedores Linux**; no es necesario cambiar el sistema operativo ni forzar `platform: linux/amd64`. Las imágenes utilizadas ofrecen variantes para las arquitecturas habituales.

## Cambios

- Cada API responde `GET /internal/health` con HTTP 200 cuando Spring MVC está listo. Es una comprobación de vida HTTP, no una prueba completa de dependencias (PostgreSQL, RabbitMQ).
- La ruta pública de salud se limita a GET en Spring Security; no modifica la autenticación de las demás rutas.
- Los tres healthchecks usan `curl` incluido explícitamente en sus imágenes, en lugar de depender de `nc`.
- Se amplió el período inicial para equipos donde la compilación o el inicio tardan más.
- El orquestador sigue publicando `localhost:8080`; los servicios internos continúan sin puertos publicados en el compose unificado.

## Requisitos

Docker Desktop abierto con el motor en ejecución y el plugin `docker compose` disponible.

## Aplicar corrección al repositorio existente

En Terminal, ubícate en la carpeta que contiene **las cuatro carpetas hermanas** `tracking-identidad-api`, `tracking-envios-api`, `tracking-rutas-api`, `tracking-orquestador-api`, y descomprime el ZIP de parche con `unzip -o`.

```bash
cd tracking-orquestador-api
# Solo si aún no existe .env:
[ -f .env ] || cp .env.example .env
# Si acabas de crear .env, edita DB_PASSWORD, RABBITMQ_PASSWORD y DBA_API_KEY.
# Si ya existían volúmenes, conserva sus credenciales originales.

docker compose config --quiet
docker compose build --no-cache identidad envios rutas
docker compose up -d --force-recreate --remove-orphans
docker compose ps
curl -fsS http://localhost:8080/health
```

Opcionalmente, ejecuta `./iniciar-macos.sh`, que realiza el procedimiento de arranque y muestra información de diagnóstico si algo falla.

**No ejecutes `docker compose down -v` ni `docker volume prune`**: eliminarías volúmenes persistentes.

## Diagnóstico si un servicio no pasa a healthy

```bash
docker inspect tracking-logistico-identidad-1 --format '{{json .State.Health}}'
docker compose logs --tail=120 identidad
docker compose ps
```

Un HTTP 200 de `/internal/health` confirma que Spring responde en ese puerto; no garantiza que las operaciones de negocio o las conexiones externas estén funcionando en ese momento. Para comprobar esas funciones hacen falta pruebas de integración.
