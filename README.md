# Training Project

Training Project is a generic Java backend intended for study, onboarding, and local experimentation. It is structured as a modular Spring Boot application with sample authentication, catalog, cart, order, review, file-storage, and integration flows.

## What this repository contains

- Java 25, Spring Boot 4, Maven
- Modular backend code under `src/main/java/com/example/trainingproject`
- OpenAPI source files under `src/main/resources/api-specs`
- Local development docs under `docs/`
- Test coverage with JUnit 5 and Testcontainers

## Local run

Prerequisites:

- Java 25
- Maven 3.9+
- Docker

Start local dependencies:

```bash
docker compose --env-file .env.example up -d postgres redis minio minio-init
```

Run the backend:

```bash
set -a && source .env.example && set +a
mvn spring-boot:run
```

Useful local endpoints:

- Health: `http://localhost:8083/actuator/health`
- Swagger UI: `http://localhost:8083/api/docs/swagger-ui/index.html`

## Project structure

```text
src/main/java/com/example/trainingproject/
├── auth/
├── cart/
├── common/
├── email/
├── favorite/
├── filestorage/
├── order/
├── payment/
├── product/
├── review/
├── security/
├── supportchat/
└── user/
```

## Notes

- This repository has been anonymized for training use.
- License files and project-specific branding were intentionally removed.
- Some third-party technology names remain where they describe real framework or integration code used by the application.
