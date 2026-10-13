#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if ! command -v docker >/dev/null 2>&1; then
  echo "Error: instala y abre Docker Desktop para macOS." >&2
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  echo "Error: el motor de Docker Desktop no está funcionando." >&2
  exit 1
fi
if ! docker compose version >/dev/null 2>&1; then
  echo "Error: no está disponible Docker Compose V2." >&2
  exit 1
fi
if [[ ! -f .env ]]; then
  cp .env.example .env
  echo "Se creó .env a partir de .env.example. Edita las credenciales antes de iniciar." >&2
  echo "Si ya existen volúmenes de PostgreSQL/RabbitMQ, usa sus credenciales originales." >&2
  exit 1
fi

docker compose config --quiet
if ! docker compose up -d --build --force-recreate --remove-orphans; then
  echo "El arranque falló. Estos son los estados y los healthchecks:" >&2
  docker compose ps || true
  docker inspect tracking-logistico-identidad-1 --format '{{json .State.Health}}' 2>/dev/null || true
  docker compose logs --tail=100 identidad 2>/dev/null || true
  exit 1
fi

docker compose ps
if ! curl --fail --silent --show-error --max-time 10 http://localhost:8080/health; then
  echo "El gateway no pasó la prueba de salud. Revisa los logs:" >&2
  docker compose logs --tail=100 orquestador identidad envios rutas || true
  exit 1
fi
echo
echo "Stack disponible por http://localhost:8080"
