# Backend principal — Tracking Logístico

Punto de acceso único al frontend, con las mismas rutas REST del backend original. No contiene ni duplica la lógica de autenticación, pedidos, rutas o trazabilidad.

## Inicio rápido (PowerShell)

Extrae las cuatro carpetas `tracking-orquestador-api`, `tracking-identidad-api`, `tracking-envios-api`, `tracking-rutas-api` en un mismo directorio (carpetas hermanas).

```powershell
cd .\tracking-orquestador-api
Copy-Item .env.example .env
notepad .env
docker compose up -d --build --remove-orphans
docker compose ps
```

Reemplaza las claves de ejemplo en `.env` (`DB_PASSWORD`, `RABBITMQ_PASSWORD` y `DBA_API_KEY`) por las usadas actualmente en tus servicios; no subas `.env` a Git.

- API frontend: `http://localhost:8080/api/v1` (sin cambios).
- Diagnóstico: `http://localhost:8080/health`.
- Prueba de conexión: `POST http://localhost:8080/api/v1/auth/login` mantiene su payload original.
- Para mostrar archivos PDF/CSV de la HU-14 sigue usando `GET /api/v1/pedidos/{id}/trazabilidad/exportar?formato=pdf|csv`.
- En desarrollo individual se pueden lanzar las APIs en 8081, 8082 y 8083; configura `IDENTIDAD_API_URL`, `ENVIOS_API_URL` y `RUTAS_API_URL` en el entorno local del orquestador.

## Seguridad y compatibilidad

El gateway no inventa sesiones ni cambia la autenticación: reenvía `Authorization` y los otros headers; los backend destino mantienen todas las validaciones de rol y token. No expongas los tres puertos internos a Internet. Para producción, pon TLS delante del gateway y limita CORS a tu dominio.

Se utiliza la BD PostgreSQL y RabbitMQ originales, con Flyway habilitado únicamente en Identidad. Los contenedores preservan las variables existentes mediante `env_file: .env`. No elimines los volúmenes existentes si necesitas conservar datos. El `docker-compose.yml` en esta carpeta es el único que hay que levantar para iniciar el conjunto.

## Validación

```powershell
.\mvnw.cmd test
docker compose config
docker compose ps
docker compose logs --tail=100 orquestador identidad envios rutas
Invoke-WebRequest http://localhost:8080/health
```
