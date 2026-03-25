# Document

_The service provides support for creating, readning, updating and deleting documents and document files attached to
them. It also has support for reading revisions of a document._

## Getting Started

### Prerequisites

- **Java 25 or higher**
- **Maven**
- **MariaDB**
- **Git**
- **[Dependent Microservices](#dependencies)**

### Installation

1. **Clone the repository:**

   ```bash
   git clone https://github.com/Sundsvallskommun/api-service-document.git
   cd api-service-document
   ```
2. **Configure the application:**

   Before running the application, you need to set up configuration settings.
   See [Configuration](#Configuration)

   **Note:** Ensure all required configurations are set; otherwise, the application may fail to start.

3. **Ensure dependent services are running:**

   If this microservice depends on other services, make sure they are up and accessible.
   See [Dependencies](#dependencies) for more details.

4. **Build and run the application:**

   - Using Maven:

     ```bash
     mvn spring-boot:run
     ```
   - Using Gradle:

     ```bash
     gradle bootRun
     ```

## Architecture

### Domain Model

Documents have a **registration number** (format: `YYYY-municipalityId-sequence`, e.g. `2023-2281-1`) and a **revision** number that increments on each update. Updates create a new revision entity (copy-on-write). Files are stored as BLOBs via a separate `DocumentDataBinaryEntity` to allow lazy loading.

### Key Paths

- **3 Resource classes** serve different endpoint groups:
  - `DocumentResource` — `/{municipalityId}/documents` (main CRUD + search + file ops)
  - `DocumentRevisionResource` — `/{municipalityId}/documents/{registrationNumber}/revisions`
  - `DocumentTypeAdministrationResource` — `/{municipalityId}/admin/documenttypes`
- **Services**: `DocumentService` (core logic), `DocumentTypeService` (cached CRUD), `RegistrationNumberService` (sequence generation)
- **Single integration**: EventLog via Feign client (`integration/eventlog/`). Models generated from `src/main/resources/integrations/eventlog-api.yaml`.

### Caching

`DocumentTypeService` uses Caffeine cache (`documentTypeCache`, 500 entries, 600s TTL) with `@Cacheable`/`@CacheEvict`. Config in `CacheConfiguration`.

### Search

Two search approaches: free-text query (LIKE on multiple fields via `SearchSpecification`) and parameterized filter via `DocumentParameters`. Both support confidentiality filtering via `InclusionFilter` enum.

### Database

MariaDB with Flyway (disabled by default). 3 migrations in `src/main/resources/db/migration/`. Integration tests use TestContainers (MariaDB 10.6.4) with Flyway enabled. Test data scripts in `src/integration-test/resources/db/scripts/`.

### Multipart Handling

Document creation/update accepts multipart requests: a JSON part (`document`) + file parts. The Resource deserializes JSON manually via `ObjectMapper`.

## Testing Structure

- `src/test/` — Unit tests. Resource tests use `@SpringBootTest` with `WebTestClient` and `@MockitoBean`.
- `src/integration-test/` — AppTests extending `AbstractAppTest` with `@WireMockAppTestSuite`. WireMock stubs in `__files/` and `mappings/`.
- `OpenApiSpecificationIT` — Contract test verifying `src/test/resources/api/openapi.yml` matches generated spec.

## Dependencies

Optional external dependency: **Eventlog** (`api-service-eventlog`) for audit logging of document operations. Controlled by `integration.eventlog.enabled` (default: `true`). When disabled, all eventlog beans (`EventLogClient`, `EventlogConfiguration`, `EventlogProperties`) are excluded via `@ConditionalOnProperty`, and `DocumentService` receives empty `Optional`s — event logging is silently skipped. All document mutations (create, update, delete, file changes) log events with 10-year expiry when enabled.

## API Documentation

Access the API documentation via Swagger UI:

- **Swagger UI:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

Alternatively, see the `openapi.yaml` file located in `src/test/resources/api` for the OpenAPI specification.

## Usage

### API Endpoints

See [API Documentation](#api-documentation) for detailed information on available endpoints.

### Example Request

```bash
curl -X 'GET' 'http://localhost:8080/2281/documents?query=searchstring%2A&onlyLatestRevision=true' -H 'accept: application/json'
```

## Configuration

Configuration is crucial for the application to run successfully. Ensure all necessary settings are configured in
`application.yml`.

### Key Configuration Parameters

- **Server Port:**

  ```yaml
  server:
    port: 8080
  ```
- **Database Settings:**

  ```yaml
  spring:
    datasource:
      url: jdbc:mysql://localhost:3306/your_database
      username: your_db_username
      password: your_db_password
  ```
- **External Service URLs:**

  ```yaml
  integration:
    service:
      url: http://dependency_service_url
      oauth2:
        client-id: some-client-id
        client-secret: some-client-secret

  service:
    oauth2:
      token-url: http://dependecy_service_token_url
  ```

### Database Initialization

The project is set up with [Flyway](https://github.com/flyway/flyway) for database migrations. Flyway is disabled by
default so you will have to enable it to automatically populate the database schema upon application startup.

```yaml
spring:
  flyway:
    enabled: true
```

- **No additional setup is required** for database initialization, as long as the database connection settings are
  correctly configured.

### Additional Notes

- **Application Profiles:**

  Use Spring profiles (`dev`, `prod`, etc.) to manage different configurations for different environments.

- **Logging Configuration:**

  Adjust logging levels if necessary.

## Contributing

Contributions are welcome! Please
see [CONTRIBUTING.md](https://github.com/Sundsvallskommun/.github/blob/main/.github/CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the [MIT License](LICENSE).

## Status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-document&metric=alert_status)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-document)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-document&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-document)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-document&metric=security_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-document)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-document&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-document)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-document&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-document)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-document&metric=bugs)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-document)

## 

Copyright (c) 2023 Sundsvalls kommun
