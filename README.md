# Hubz

<!-- Replace OWNER/hubz with your actual GitHub owner/repo -->
[![Backend Tests](https://github.com/OWNER/hubz/actions/workflows/backend-tests.yml/badge.svg)](https://github.com/OWNER/hubz/actions/workflows/backend-tests.yml)
[![Frontend Tests](https://github.com/OWNER/hubz/actions/workflows/frontend-tests.yml/badge.svg)](https://github.com/OWNER/hubz/actions/workflows/frontend-tests.yml)
[![Build](https://github.com/OWNER/hubz/actions/workflows/build.yml/badge.svg)](https://github.com/OWNER/hubz/actions/workflows/build.yml)
[![codecov](https://codecov.io/gh/OWNER/hubz/graph/badge.svg)](https://codecov.io/gh/OWNER/hubz)

A productivity app for managing organizations, teams, tasks, goals, and personal habits.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 21, Spring Boot 3.2+, Spring Security + JWT, Spring Data JPA |
| **Frontend** | React 18, TypeScript, Vite, TailwindCSS, Zustand |
| **Database** | PostgreSQL 16 (prod), H2 (dev) |
| **CI/CD** | GitHub Actions, Docker, Dependabot |

## Getting Started

### Prerequisites

- Java 21 (JDK)
- Node.js 20+
- Docker & Docker Compose (optional)

### Backend

```bash
cd hubz-backend
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

### Frontend

```bash
cd hubz-frontend
npm install
npm run dev
```

The app will be available at `http://localhost:5173`.

### Docker

```bash
docker-compose up -d
```

This starts PostgreSQL, the backend, and the frontend. The app will be available at `http://localhost:3000`.

## Running Tests

### Backend

```bash
cd hubz-backend
./mvnw test                    # Run tests
./mvnw test jacoco:report      # Run tests with coverage report
```

### Frontend

```bash
cd hubz-frontend
npm run test                   # Run tests in watch mode
npm run test:run               # Run tests once
npm run test:coverage          # Run tests with coverage
```

## Architecture

Hubz follows **Clean Architecture** with 4 strict layers:

```
Presentation  -->  Application  -->  Domain
                       |
                 Infrastructure
```

- **Domain**: Pure Java models, enums, business logic (no framework dependencies)
- **Application**: Services, DTOs, use case interfaces, repository ports
- **Infrastructure**: JPA entities, repository adapters, security, external APIs
- **Presentation**: REST controllers, exception handlers

See [CLAUDE.md](CLAUDE.md) for detailed architecture documentation.

## Features

See [FEATURES.md](FEATURES.md) for the complete feature list and status.

## License

Private project.
