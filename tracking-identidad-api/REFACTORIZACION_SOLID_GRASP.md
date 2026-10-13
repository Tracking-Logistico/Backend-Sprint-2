# Arquitectura y responsabilidades (refactorización)

Este sistema dispone de un único punto de entrada HTTP para el frontend: `tracking-orquestador-api` en `/api/v1` (puerto 8080). No se modificaron los mappings de los controladores REST, los DTO públicos ni las variables de entorno. El orquestador mantiene la delegación a Identidad (8081), Envíos (8082) y Rutas (8083).

## Separación de responsabilidades

- `PedidoService` mantiene `PedidoServiceI` y actúa solo como fachada compatible. `RecepcionPedidoUseCase` gestiona recepción y corrección; `ConsultaPedidoUseCase`, lectura e historial; `ValidacionPedidoUseCase`, aprobación/rechazo; `OperacionLogisticaPedidoUseCase`, tracking, estados y etiquetas. Cada caso de uso define sus propias transacciones Spring.
- `PedidoMapeador` transforma el dominio a DTO. `AutorizacionLecturaPedido` concentra las políticas de lectura. `RegistroHistorialPedido` sigue registrando eventos sin alterar el contrato de trazabilidad HU-14.
- `AutenticacionService` conserva `AutenticacionServiceI`, pero delega a `InicioSesionUseCase`, `GestionSesionesUseCase` y `RestablecimientoCredencialesUseCase`. `TokensSeguros` genera y calcula los hashes; el método `AutenticacionService.hash` sigue disponible como adaptador de compatibilidad.
- `RutaService` conserva `RutaServiceI`, pero delega el mapeo en `RutaMapeador` y la política de capacidad en `ValidadorCapacidadRuta`. Se comunica con pedidos mediante el puerto `EnviosParaRutaPort` y un `AdaptadorEnviosParaRuta`, que evita acoplar las reglas de rutas a toda la interfaz de pedidos.
- El gateway implementa enrutamiento (`Enrutador`), coordinación de entrada (`CoordinadorSolicitudes`) y transporte (`TransporteHttp` / `TransporteHttpCliente`). No contiene lógica de negocio ni persiste datos.

## Principios

- **SRP (SOLID)**: cada caso de uso, política, mapeador y adaptador tiene una causa principal de cambio.
- **DIP (SOLID)**: el módulo de Rutas depende de un puerto de operaciones de pedidos; el orquestador depende de un puerto de transporte HTTP.
- **Controller, Indirection, Low Coupling, High Cohesion (GRASP)**: controladores delegan casos de uso; las interfaces y adaptadores aíslan dependencias; cada responsabilidad se concentra en una clase específica.
- **Arquitectura orientada a eventos**: los eventos locales y la integración con RabbitMQ existentes permanecen. La HU-14 y sus migraciones se mantienen.

## Limitaciones conocidas

La independencia todavía **no es total**: las tres aplicaciones conservan una copia del módulo `core` y acceden a la misma base de datos PostgreSQL, para evitar romper el contrato existente. El puerto `EnviosParaRutaPort` adapta a `PedidoServiceI` local; no sustituye esta dependencia por llamadas HTTP. La solución se describe correctamente como **tres APIs REST desplegables sobre un núcleo modular compartido, con gateway y mensajería**, no como tres microservicios autónomos ni como un monolito único.

Los cambios mantienen las reglas y respuestas públicas; sin embargo, la equivalencia de comportamiento no está garantizada hasta ejecutar las pruebas Maven y probar el despliegue Docker en el entorno objetivo.

## Validación y arranque

Desde `tracking-orquestador-api`:

```powershell
Copy-Item .env.example .env
notepad .env
docker compose up -d --build --remove-orphans
docker compose ps
Invoke-RestMethod http://localhost:8080/health
```

En cada API de negocio:

```powershell
.\mvnw.cmd -pl api -am test "-Dspring.profiles.active=test"
```

En el orquestador:

```powershell
.\mvnw.cmd test
```

Si quieres conservar una base de datos de desarrollo existente, revisa los volúmenes antes de usar `docker compose down -v`: ese comando eliminaría sus datos. Nunca subas `.env` con contraseñas reales al repositorio.
