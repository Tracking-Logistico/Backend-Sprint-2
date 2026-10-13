#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
for api in tracking-identidad-api tracking-envios-api tracking-rutas-api; do
  echo "=== Compilando $api"
  (cd "$api" && ./mvnw -B -pl api -am -Dmaven.test.skip=true -Djacoco.skip=true package)
done
echo "=== Compilando tracking-orquestador-api"
(cd tracking-orquestador-api && ./mvnw -B -Dmaven.test.skip=true package)
echo "=== Compilando frontend"
(cd tracking-logistico-frontend && npm ci && npm run build)
echo "COMPLETADO: todas las compilaciones terminaron sin errores"
