# Application config — port usage examples

devports is **framework-agnostic**. It does not know about Spring (or any
framework); it simply scans config files (recursively) for port placeholders of
the form `${NAME_PORT:-default}` / `${NAME_PORT:default}` and treats the default
as the base port. The principle is the same everywhere: **the app reads its
ports from environment variables**, and the generated `.env` supplies them.

Below are reference snippets per ecosystem. Use the **same env-var names** the
compose file uses for shared services (DB, broker, tracing endpoints) so one
`.env` drives both sides.

## Spring Boot — `application.yaml`

Spring placeholders use a **single colon** for the default (`${VAR:default}`).
Do **not** use `${VAR:-default}` here — Spring would treat the default as the
literal `-default`.

```yaml
server:
  port: ${APP_PORT:3773}
spring:
  datasource:
    url: jdbc:postgresql://localhost:${POSTGRES_PORT:6446}/app_db
management:
  otlp:
    tracing:
      endpoint: http://localhost:${JAEGER_OTLP_HTTP_PORT:4318}/v1/traces
```

## Spring Boot — `application.properties`

```properties
server.port=${APP_PORT:3773}
spring.datasource.url=jdbc:postgresql://localhost:${POSTGRES_PORT:6446}/app_db
management.otlp.tracing.endpoint=http://localhost:${JAEGER_OTLP_HTTP_PORT:4318}/v1/traces
```

## Docker Compose (the source of truth for service ports)

Compose interpolation uses `${VAR:-default}` (shell style, with the dash):

```yaml
services:
  postgres:
    ports:
      - "${POSTGRES_PORT:-6446}:5432"
```

## Node / other runtimes

Code-based apps usually read env vars directly rather than via config
placeholders, so there is nothing in a file for devports to scan — just make the
app read the same names the compose file declares:

```js
const port = Number(process.env.APP_PORT ?? 3773)
const dbPort = Number(process.env.POSTGRES_PORT ?? 6446)
```

In this case, declare the ports in a compose file (even for services the app
only connects to) so `allocate` can discover them; the app then picks them up
from the exported `.env`.

## The export caveat (all of the above)

`docker compose` auto-loads `.env`, but a **host process you launch yourself**
(Maven, Gradle, node, …) does not. Export the file first:

```bash
set -a && . ./.env && set +a && ./mvnw spring-boot:run
```
