# Real-time Product Catalog API

Microservice for managing an e-commerce product catalog with:

- **Product CRUD** via REST (PostgreSQL is the source of truth)
- **Fast full-text search** across product characteristics (Elasticsearch projection)
- **Price synchronization** from an external system (push endpoint, idempotent + `effectiveAt` ordering)
- **Reliable event publishing** to Kafka (Outbox pattern → Kafka → Avro + Schema Registry)
- **Flyway** database migrations
- **Profile-based configuration** (`local` / `prd`)

---

## Prerequisites

### Required

1. **Docker**
    - Used to run PostgreSQL, Elasticsearch, Kafka, Schema Registry, AKHQ, and Kibana locally.
    - Install:
        - Docker Desktop (macOS/Windows): https://www.docker.com/products/docker-desktop/
        - Docker Engine (Linux): https://docs.docker.com/engine/install/

2. **Docker Compose**
    - Usually included with Docker Desktop.
    - Install: https://docs.docker.com/compose/install/

3. **PostgreSQL (Optional – Only if NOT using Docker)**
    - The project runs PostgreSQL automatically via Docker.
    - You only need a local installation if you do not use Docker.
    - Download: https://www.postgresql.org/download/
    - Required version: PostgreSQL 16+

4. **Java 21**
    - The project is configured for Java 21.
    - Recommended distributions:
        - OpenJDK: https://openjdk.java.net/install/ ~ https://jdk.java.net/archive/
        - Oracle JDK: https://www.oracle.com/java/technologies/downloads/

5. **Git** (Optional but recommended)
    - https://git-scm.com/downloads/

The project includes the Maven Wrapper (`./mvnw`), so Maven does not need to be installed globally.

---

## Quick Start

```bash
# 1) Start local infrastructure
docker compose -f local-docker/docker-compose.yml up -d

# 2) Run the service
chmod +x mvnw
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### First Start (Important)

On the first startup, you must generate the Avro classes before running the application:

```bash
./mvnw clean compile
```

The Avro plugin generates the event classes used for Kafka serialization/deserialization.
Simply starting the application without compiling first may result in missing class errors.


### Service endpoints:

- API: http://localhost:8080
- AKHQ: http://localhost:8090
- Schema Registry: http://localhost:8081
- Elasticsearch: http://localhost:9200
- Kibana: http://localhost:5601
- Postgres: localhost:5432
- Kafka: localhost:9092

### Local Development Credentials

#### PostgreSQL

Host: localhost  
Port: 5432  
Database: catalog

Application User:
- Username: catalog_user
- Password: test1234

Flyway Admin User:
- Username: admin_user
- Password: test123456

#### Elasticsearch

URL: http://localhost:9200  
Username: elastic  
Password: catalog

Kibana:
- URL: http://localhost:5601
- Uses Elasticsearch credentials

*** These credentials are for local development only. Production environments must use secure environment variables. ***
---

## What the Application Does

### Product Management
Create, read, update and delete products via REST.

Each product contains:
- name
- description
- price + currency
- arbitrary attributes (key/value pairs)


#### HTTP Examples

Create a product:

```http
POST http://localhost:8080/products
Content-Type: application/json
```
```Json
{
  "name": "Tesla Model S",
  "description": "Electric luxury sedan",
  "price": 8999999,
  "currency": "EUR"
}
```

Get product by id:

```http
GET http://localhost:8080/products/d537a2b5-0cbb-46d8-af81-2a1ecf4f47f9
```

Update a product:

```http
PUT http://localhost:8080/products/d537a2b5-0cbb-46d8-af81-2a1ecf4f47f9
Content-Type: application/json
```
```Json
{
  "name": "Tesla Model S Plaid",
  "description": "Updated description 1",
  "currency": "EUR",
  "attributes": {
    "color": "red",
    "paint": "metallic"
  }
}
```

Update product price:

```http
PATCH http://localhost:8080/products/d537a2b5-0cbb-46d8-af81-2a1ecf4f47f9/price
Content-Type: application/json
```
```Json
{
  "price": 99663323,
  "currency": "EUR"
}
```

Delete a product:

```http
DELETE http://localhost:8080/products/b9153eae-ae78-4458-a5e9-888d834cfbdc
```


### Search
Full-text search across:
- name
- description
- attributes
- price and currency

Backed by Elasticsearch.


#### HTTP Examples

Basic search:

```http
GET http://localhost:8080/products/search?query=Tesla&size=20
```

Cursor-based pagination:

```http
GET http://localhost:8080/products/search?query=shoe&size=20&cursor=YOUR_CURSOR_HERE
```


#### Important: ReIndex Requirement

PostgreSQL is the source of truth, and Elasticsearch is maintained as a projection via Kafka events (Outbox → Kafka → Elasticsearch consumer).

If product data is inserted or modified **directly in PostgreSQL** (for example via SQL scripts, manual database changes, or bulk imports) the corresponding Kafka events will not be produced. As a result, Elasticsearch will not be updated automatically.

Before using the search endpoint in such cases, you must trigger a manual reindex to synchronize Elasticsearch with PostgreSQL.

Example endpoint:

```
POST /admin/reindex
```


Example (with batch size):

```http
POST http://localhost:8080/admin/reindex?batchSize=1000
```


This endpoint rebuilds the Elasticsearch index from the current state of PostgreSQL.

Use this endpoint when:
- Bootstrapping a new environment with preloaded database data
- Performing bulk imports directly into PostgreSQL
- Recovering from projection inconsistencies

In normal application flows (using the REST API), reindexing is **not required**, because all changes are propagated via Kafka.

### Price Synchronization

External systems can push price updates using:

```
POST /price-integration/prices
```

Supports:
- Idempotency via `requestId`
- Ordering via `effectiveAt`
- No duplicate events when price does not change


#### HTTP Example

```http
POST http://localhost:8080/price-integration/prices
Content-Type: application/json
```

```Json
{
  "requestId": "req-0004",
  "productId": "b9153eae-ae78-4458-a5e9-888d834cfbdc",
  "priceInCents": 123456789,
  "currency": "EUR",
  "effectiveAt": "2026-02-23T21:05:00Z",
  "source": "price-integration"
}
```


---

## Kafka & Avro

### Topics

- product-events
- product-events.DLT

### Avro Message Examples

All messages use:
- Key: Product UUID
- occurredAt: epoch millis

---

### ProductCreatedEvent

Key:
```
111e4567-e89b-12d3-a456-426614174000
```

Value:
```json
{
  "eventId": "c1111111-e89b-12d3-a456-426614174000",
  "eventVersion": 1,
  "occurredAt": 1735689600000,
  "productId": "111e4567-e89b-12d3-a456-426614174000",
  "name": "Gaming Mouse",
  "description": "High precision wireless gaming mouse",
  "priceInCents": 5999,
  "currency": "EUR",
  "attributes": {
    "color": "black",
    "dpi": "16000",
    "connection": "wireless"
  }
}
```

---

### ProductUpdatedEvent

```json
{
  "eventId": "u2222222-e89b-12d3-a456-426614174000",
  "eventVersion": 1,
  "occurredAt": 1735689600000,
  "productId": "111e4567-e89b-12d3-a456-426614174000",
  "name": "Gaming Mouse Pro",
  "description": null,
  "attributes": {
    "dpi": "20000"
  }
}
```

---

### ProductPriceUpdatedEvent

```json
{
  "eventId": "p3333333-e89b-12d3-a456-426614174000",
  "eventVersion": 1,
  "occurredAt": 1735689600000,
  "productId": "111e4567-e89b-12d3-a456-426614174000",
  "oldPriceInCents": 5999,
  "newPriceInCents": 6499,
  "currency": "EUR"
}
```

---

### ProductDeletedEvent

```json
{
  "eventId": "d4444444-e89b-12d3-a456-426614174000",
  "eventVersion": 1,
  "occurredAt": 1735689600000,
  "productId": "111e4567-e89b-12d3-a456-426614174000"
}
```

---

## Build & Run

```bash
./mvnw clean package
java -jar target/*.jar --spring.profiles.active=local
```

Run tests:

```bash
./mvnw test
```

---

## Troubleshooting

### Port Conflicts

Ensure ports 5432, 9092, 8081, 8090, 9200, 5601 are free.

### Reset Local Environment

```
docker compose -f local-docker/docker-compose.yml down -v
docker compose -f local-docker/docker-compose.yml up -d
```

---

### Elasticsearch Authentication Issues

Docker Compose gives priority to shell environment variables over `.env`.

If Elasticsearch credentials do not match the `.env` file,
verify that no system-level `ELASTIC_PASSWORD` variable is set.

PowerShell:
```
echo $env:ELASTIC_PASSWORD
```

If a value is returned:
- Remove the environment variable
- Restart your terminal or IDE
- Recreate containers

```
docker compose down -v
docker compose up -d
```
---

## License

MIT.
