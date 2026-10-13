# Backend principal / Orquestador REST

```text
Frontend (VITE_API_URL=http://localhost:8080/api/v1)
        |
        v
[Orquestador Java Spring Boot :8080]
        |-- /api/v1/auth, clientes, usuarios, notificaciones, admin --> [Identidad :8081]
        |-- /api/v1/pedidos/** ------------------------------> [Envios :8082]
        `-- /api/v1/rutas/**, conductor/panel/** -----------> [Rutas :8083]
                   |                  |                   |
                   +---- PostgreSQL compartido ------------+
                   +------ RabbitMQ (eventos) -------------+
```

## Responsabilidades

- `Enrutador`: responsabilidad única de mapear prefijos antiguos al servicio correcto.
- `CoordinadorSolicitudes`: delega la solicitud y desconoce los detalles de HTTP cliente.
- `TransporteHttp`: abstracción (inversión de dependencias); `TransporteHttpCliente`: adaptador JDK HttpClient.
- `EntradaRestController`: adaptador REST de entrada; las APIs internas autorizan y ejecutan la lógica original.
- `SaludServiciosController`: muestra conectividad de puertos de cada API (no hace comprobación de BD).
- `ConfiguracionCors`: origen del frontend autorizado en un solo punto.

REST síncrono para operaciones y respuestas; RabbitMQ para eventos asincrónicos de negocio. El orquestador no persiste entidades ni comparte base de datos. Los tres servicios existentes comparten su núcleo Java y una PostgreSQL; no son microservicios de datos aislados y no hay transacciones distribuidas.

## Garantías y límites

Se conservan método, ruta, query string, cabeceras permitidas (Authorization, Cookie, X-DBA-Key), bytes de cuerpo, estado, Content-Disposition y Content-Type. Las llamadas HTTPS al orquestador deben terminar TLS en un reverse proxy antes del puerto 8080. Se eliminan cabeceras hop-by-hop y CORS duplicadas por reglas HTTP.

Solo se publica el puerto 8080 hacia el exterior. PostgreSQL, RabbitMQ, Identidad, Envíos y Rutas quedan accesibles exclusivamente entre contenedores del Compose. El frontend sigue usando `http://localhost:8080/api/v1`, sin cambiar paths.

No se han sustituido flujos de transacción con sagas ni se ha garantizado entrega exactamente una vez de eventos RabbitMQ. Para producción, configurar secretos por plataforma, TLS, backups, observabilidad y políticas de reintento/idempotencia.
