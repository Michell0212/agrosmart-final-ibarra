# AgroSmart — Plataforma de Comercialización Agrícola

Examen final integrador de Programación Avanzada (ESPE). API reactiva construida con
Spring Boot 3.5.6 (WebFlux), Spring Data JPA sobre PostgreSQL 16 y un módulo de IA con
LangChain4j para generar publicidad de productos.

**Autora:** Milena Michell Ibarra Díaz — `mmibarra2@espe.edu.ec`

---

## Semilla personal

Los parámetros del examen se derivan de los dos últimos dígitos de mi cédula
(**1757660483** → `NN = 83`):

| Parámetro | Cómo se obtiene | Mi valor |
|---|---|---|
| Nombre de la tabla | `tbl_productos_base_` + `NN` | `tbl_productos_base_83` |
| Puerto del servidor | `81` + `NN` | `8183` |
| Categoría | según el último dígito (3 → Café) | Café |
| Audiencia del prompt de IA | según la categoría | Cafeterías de especialidad |

La categoría **no** forma parte del nombre de la tabla: define los productos que se
siembran dentro (cafés) y la audiencia del prompt de IA.

---

## Requisitos

- JDK 21
- Docker Desktop (para el contenedor de PostgreSQL)
- Maven (se incluye el wrapper `mvnw`)

---

## Cómo ejecutar

La base de datos se levanta automáticamente mediante Docker Compose Support de Spring
Boot: no hay que crearla a mano. El archivo `compose.yaml` define el contenedor de
PostgreSQL 16.

1. Iniciar Docker Desktop y verificar que responde:

   ```bash
   docker ps
   ```

2. Arrancar la aplicación:

   ```bash
   ./mvnw spring-boot:run
   ```

   Spring levanta el contenedor `agrosmart-postgres`, crea la tabla
   `tbl_productos_base_83` (con `ddl-auto=update`) y siembra 5 cafés (3 válidos + 2
   inválidos) de forma idempotente. El servidor queda escuchando en el puerto **8183**.

3. Verificar en el log de arranque:

   ```
   The following 1 profile is active: "prod"
   Netty started on port 8183 (http)
   ```

### Configuración

El perfil `prod` (`application.properties` → `spring.profiles.active=prod`) carga
`application-prod.properties`, donde vive el puerto `8183` y la configuración del modelo
de IA. **No se declara `spring.datasource.*`**: Docker Compose Support lee el contenedor
en ejecución y construye el JDBC URL automáticamente.

> **Nota de entorno (Windows):** si el arranque falla con *"Port 8183 was already in
> use"* sin que ningún proceso escuche ese puerto, es porque Hyper-V/WSL2 reservan rangos
> TCP dinámicos. Se resuelve reservando el puerto explícitamente desde una consola
> elevada (ver `DECISIONES.md`, incidente de infraestructura de Fase 1).

---

## Endpoints

Con la aplicación corriendo, las respuestas reales de terminal:

### `GET /api/productos` — productos comercializables

Devuelve un `Flux<Producto>` con los cafés válidos (precio > 0 y con correos). Los nombres
salen en mayúsculas porque el pipeline aplica `A_MAYUSCULAS`.

```bash
$ curl.exe http://localhost:8183/api/productos
[{"id":1,"nombre":"CAFÉ ARÁBICA LAVADO ALTA MONTAÑA","categoria":"Café","precioUsd":12.50,"correosNotificacion":["compras@cafeteria.ec","barista@cafeteria.ec"]},{"id":2,"nombre":"CAFÉ GEISHA NATURAL MICROLOTE","categoria":"Café","precioUsd":28.00,"correosNotificacion":["ventas@especialidad.ec"]},{"id":3,"nombre":"CAFÉ HONEY PROCESS ORIGEN ÚNICO","categoria":"Café","precioUsd":18.75,"correosNotificacion":["pedidos@cafeteria.ec"]}]
```

### `GET /api/productos/{id}` — producto por id

Devuelve un `Mono<Producto>`. Si el id no existe, responde **404**.

```bash
$ curl.exe http://localhost:8183/api/productos/1
{"id":1,"nombre":"Café Arábica Lavado Alta Montaña","categoria":"Café","precioUsd":12.50,"correosNotificacion":["compras@cafeteria.ec","barista@cafeteria.ec"]}

$ curl.exe -i http://localhost:8183/api/productos/9999
HTTP/1.1 404 Not Found
Content-Type: application/json
{"timestamp":"...","path":"/api/productos/9999","status":404,"error":"Not Found"}
```

### `GET /api/agrosmart/publicidad` — publicidad generada por IA

Recibe `producto` y `audiencia` por `@RequestParam` y devuelve el texto plano generado por
el modelo.

```bash
$ curl.exe "http://localhost:8183/api/agrosmart/publicidad?producto=Cafe%20arabigo%20de%20altura&audiencia=cafeterias%20de%20especialidad"
"Descubre el café arábigo de altura: un sabor único que cautivará a tus clientes y elevará tu menú."
```

---

## El puente bloqueante → reactivo

El punto central de la arquitectura. Spring Data JPA es **bloqueante**: `findAll()` deja el
hilo esperando la respuesta de PostgreSQL. WebFlux, en cambio, atiende las peticiones con
un pool reducido de hilos del *event loop* de Netty (`reactor-http-nio-*`), y esos hilos
**nunca deben bloquearse**: si uno se detiene, degrada todas las peticiones concurrentes.

El puente es `Mono.fromCallable(...)` combinado con
`subscribeOn(Schedulers.boundedElastic())`:

- **`fromCallable`** difiere la ejecución de la consulta hasta que hay un suscriptor (a
  diferencia de `Mono.just(repository.findAll())`, que la ejecutaría de inmediato en el
  hilo llamante).
- **`boundedElastic`** traslada esa ejecución a un pool separado, diseñado para tareas
  bloqueantes. Sin él, la consulta correría en el event loop y Reactor lanzaría
  `BlockingOperationError`.

La llamada al modelo de IA usa el mismo puente, porque una petición HTTP a un proveedor
externo también bloquea el hilo mientras espera la respuesta.

---

## Operadores reactivos usados

En `ProductoService.obtenerProductosComercializables()`:

| Operador | Por qué está |
|---|---|
| `Mono.fromCallable(repo::findAll)` | Difiere la consulta bloqueante; no se ejecuta hasta que hay suscriptor. |
| `subscribeOn(boundedElastic())` | Ejecuta el bloqueo fuera del event loop de Netty. |
| `flatMapMany(Flux::fromIterable)` | Convierte la lista materializada en un flujo que emite uno a uno. |
| `map(ProductoMapper::toDominio)` | Convierte cada entidad JPA al modelo de dominio inmutable. |
| `map(A_MAYUSCULAS)` | Normaliza el nombre a mayúsculas devolviendo una instancia nueva. |
| `filter(IS_VALID)` | Descarta los productos no comercializables (precio ≤ 0 o sin correos). |
| `doOnNext(LOG_PRODUCTO)` | Efecto de trazabilidad, sin transformar el elemento. |
| `defaultIfEmpty(...)` | Emite un producto genérico si el filtro dejó el flujo vacío. |

En `ProductoService.buscarPorId(Long id)`:

| Operador | Por qué está |
|---|---|
| `Mono.fromCallable(() -> repo.findById(id))` | Difiere la búsqueda bloqueante. |
| `subscribeOn(boundedElastic())` | Aísla el bloqueo del event loop. |
| `flatMap(Mono::justOrEmpty)` | `Optional` vacío → `Mono` vacío. |
| `switchIfEmpty(Mono.error(...))` | Si no existe el id, propaga `ProductoNoEncontradoException` (→ HTTP 404). |

**`defaultIfEmpty` vs `switchIfEmpty`:** el primero entrega un valor de reemplazo (un
producto centinela) cuando la lista queda vacía; el segundo sustituye el flujo vacío por
otro publisher (un `Mono.error`) para propagar el 404. No son intercambiables.

---

## Estructura del proyecto

```
src/main/java/ec/edu/espe/agrosmart/
├── AgrosmartApplication.java          # main + @Bean CommandLineRunner (siembra)
├── controller/AgroSmartController.java
├── service/ProductoService.java
├── service/AgroSmartAIService.java    # interfaz @AiService (LangChain4j)
├── service/PublicidadService.java
├── repository/ProductoRepository.java
├── entity/ProductoEntity.java
├── domain/Producto.java               # modelo inmutable
├── domain/ProductoFilters.java        # Predicate / Consumer / Function
├── mapper/ProductoMapper.java
└── exception/ProductoNoEncontradoException.java
```

---

## Pruebas

```bash
./mvnw test
```

11 pruebas unitarias con JUnit 5, Mockito y StepVerifier, sin depender de PostgreSQL ni de
internet: `ProductoTest` (getters y copias defensivas), `ProductoFiltersTest` (caso válido
+ dos inválidos), `ProductoServiceTest` (flujo reactivo con StepVerifier) y
`PublicidadServiceTest` (camino feliz y fallo del proveedor de IA).

---

## Documentación adicional

- **`DECISIONES.md`** — bitácora de diseño en primera persona, una entrada por fase.
- **`IDENTIDAD.md`** — datos del examen, semilla y enlace al video de defensa.
- **`docs/evidencias/`** — capturas de arranque, `psql`, los 4 `curl`, pruebas en verde y
  `git log`.
