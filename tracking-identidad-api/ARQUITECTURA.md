# Arquitectura y límites — 01 · API de identidad y usuarios

```text
                  Internet / frontend
                         |
                  Proxy/API Gateway
           +-------------+-------------+
           |             |             |
       API Identidad  API Envíos      API Rutas
         :8081          :8082           :8083
           |             |             |
           +------- PostgreSQL --------+
           +-------- RabbitMQ ---------+
                   logistica.eventos
```

Contextos lógicos separados por controlador, servicios de aplicación, dominio e infraestructura.
Los tres ZIP mantienen el core completo debido a asociaciones JPA existentes y contratos compartidos,
pero exponen únicamente los controladores de su dominio. El acoplamiento a BD y las dependencias
cruzadas se conservan deliberadamente para no alterar la semántica de transacciones original.

- Identidad es dueña de autenticación, sesiones y consumidor de notificaciones.
- Envíos es dueño de pedidos, incidencias, historial (HU-14), checkpoints y escalamiento.
- Rutas es dueña del panel, asignaciones y cierre de paradas; consume eventos del módulo pedidos.
- El productor publica tras commit; los consumidores usan colas duraderas. La ausencia de transactional
  outbox es una limitación explícita. Evita prometer entrega garantizada ante caída del broker.
- El único propietario del esquema Flyway en despliegue conjunto es Identidad.

El gateway Nginx local (empaquetado con Identidad) conserva una URL de entrada única en :8080.

Distribuir tres procesos implica una arquitectura de servicios con núcleo modular y persistencia
compartida, no un único monolito modular. Para separar completamente los contextos, se requeriría
sustituir las dependencias JPA cruzadas por contratos remotos, y separar propietarios de datos.
