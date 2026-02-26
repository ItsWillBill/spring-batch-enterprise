# 🏦 Transaction Reconciliation Engine

> A production-grade financial transaction reconciliation system built with **Spring Boot 3**, **Spring Batch 5**, **Java 21**, **PostgreSQL**, and **Redis**.  
> Designed to process millions of records reliably, with full restart/recovery, audit trails, and cloud-native deployment.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)](https://spring.io/projects/spring-boot)
[![Spring Batch](https://img.shields.io/badge/Spring%20Batch-5.x-green)](https://spring.io/projects/spring-batch)

---

## 📋 Table of Contents

- [🏦 Transaction Reconciliation Engine](#-transaction-reconciliation-engine)
  - [📋 Table of Contents](#-table-of-contents)
  - [💼 Business Case](#-business-case)
  - [🏗 Architecture](#-architecture)
    - [Hexagonal Architecture (Ports \& Adapters)](#hexagonal-architecture-ports--adapters)
  - [🛠 Tech Stack](#-tech-stack)
  - [🚀 Getting Started](#-getting-started)
    - [Prerequisites](#prerequisites)
    - [1. Clone and configure](#1-clone-and-configure)
    - [2. Run with Docker (recommended)](#2-run-with-docker-recommended)
    - [3. Run locally (without Docker)](#3-run-locally-without-docker)
    - [4. Run tests](#4-run-tests)
  - [🔐 Configuration \& Secrets](#-configuration--secrets)
  - [⚙️ How It Works](#️-how-it-works)
    - [Batch Job Flow](#batch-job-flow)
    - [Reconciliation Logic](#reconciliation-logic)
    - [CSV Format](#csv-format)
  - [🧱 SOLID Principles Applied](#-solid-principles-applied)
  - [☕ Java 21 Features Used](#-java-21-features-used)
  - [🗺 Development Roadmap](#-development-roadmap)
    - [✅ Phase 1 — Foundation (Current)](#-phase-1--foundation-current)
    - [🚧 Phase 2 — Resilience \& Logic (Upcoming)](#-phase-2--resilience--logic-upcoming)
    - [🔮 Phase 3 — Scale \& Performance (Planned)](#-phase-3--scale--performance-planned)
  - [🔄 CI/CD](#-cicd)

---

## 💼 Business Case

Every financial institution — banks, fintechs, payment processors, e-commerce platforms — must **reconcile transactions daily**. The process answers one question:

> _Does what the bank says happened match what our internal ledger says happened?_

**Unreconciled transactions mean:**

- Lost revenue
- Regulatory non-compliance (SOX, PSD2)
- Failed audits
- Fraud going undetected

This engine automates that process at scale. It ingests raw transaction files (CSV, API, MQ), matches them against internal ledger entries, classifies each outcome (MATCHED / DISCREPANCY / MISSING), and produces an auditable result set — all as a resumable, fault-tolerant batch job.

**Target clients:** Fintechs, payment processors, banks, insurance companies, SaaS billing platforms.

---

## 🏗 Architecture

### Hexagonal Architecture (Ports & Adapters)

**Why Hexagonal Architecture for a batch system?**

- The same business logic must be testable without a database, without Spring, and without CSV files.
- Input sources change over time (CSV today → REST API tomorrow → Kafka next year). With hexagonal architecture, swapping the reader adapter requires zero changes to the domain.
- The domain layer can be unit tested at microsecond speed — critical for a system that may process millions of records.

---

## 🛠 Tech Stack

| Concern             | Technology                  | Version |
| ------------------- | --------------------------- | ------- |
| Language            | Java                        | 21      |
| Framework           | Spring Boot                 | 3.3.x   |
| Batch Processing    | Spring Batch                | 5.x     |
| Persistence         | Spring Data JPA + Hibernate | 6.x     |
| Database            | PostgreSQL                  | 16      |
| Cache / Distributed | Redis (Lettuce client)      | 7.2     |
| Schema Migrations   | Flyway                      | 10.x    |
| CSV Parsing         | OpenCSV                     | 5.9     |
| Containerization    | Docker + Docker Compose     | -       |
| CI/CD               | GitHub Actions              | -       |
| Testing             | JUnit 5 + Testcontainers    | -       |
| Build               | Maven                       | 3.9+    |

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Docker + Docker Compose
- Maven 3.9+

### 1. Clone and configure

```bash
git clone https://github.com/ItsWillBill/spring-batch-enterprise.git
cd reconciliation-engine

# Set up environment variables (NEVER skip this)
cp .env.example .env
# Edit .env with your local credentials
```

### 2. Run with Docker (recommended)

```bash
# Start PostgreSQL + Redis + Application
docker-compose up --build

# In another terminal, copy a CSV file to the input directory
cp data/input/sample_transactions.csv data/input/transactions.csv

# The job is triggered by passing the filename as an argument
# (See docker-compose.yml for how to pass CMD args)
```

### 3. Run locally (without Docker)

```bash
# Start PostgreSQL and Redis separately (or use docker-compose for infra only)
docker-compose up postgres redis

# Activate local profile (reads application-local.yml)
export SPRING_PROFILES_ACTIVE=local

# Run the application with a CSV file argument
./mvnw spring-boot:run -Dspring-boot.run.arguments="sample_transactions.csv"
```

### 4. Run tests

```bash
./mvnw test
```

---

## 🔐 Configuration & Secrets

**Zero credentials are hardcoded in this project.** All sensitive values are injected via environment variables.

| Variable            | Description                       | Default         |
| ------------------- | --------------------------------- | --------------- |
| `POSTGRES_HOST`     | PostgreSQL hostname               | `localhost`     |
| `POSTGRES_PORT`     | PostgreSQL port                   | `5432`          |
| `POSTGRES_DB`       | Database name                     | _(required)_    |
| `POSTGRES_USER`     | Database username                 | _(required)_    |
| `POSTGRES_PASSWORD` | Database password                 | _(required)_    |
| `REDIS_HOST`        | Redis hostname                    | `localhost`     |
| `REDIS_PORT`        | Redis port                        | `6379`          |
| `REDIS_PASSWORD`    | Redis password (empty = no auth)  | `""`            |
| `APP_INPUT_DIR`     | CSV input directory               | `/data/input`   |
| `APP_ARCHIVE_DIR`   | Processed files archive directory | `/data/archive` |
| `BATCH_CHUNK_SIZE`  | Records per chunk (tune for perf) | `500`           |

**Setup:**

```bash
cp .env.example .env   # Copy template
# Edit .env — this file is gitignored
```

---

## ⚙️ How It Works

### Batch Job Flow

```
START
  │
  ▼
[Step 1: reconciliationStep]  ← Chunk-oriented
  │  Read CSV row (Transaction)
  │  Process → ReconcileTransactionUseCase → ReconciliationResult
  │  Write chunk to PostgreSQL
  │  Repeat until file exhausted
  │
  ▼ (on SUCCESS)
[Step 2: archiveFileStep]     ← Tasklet
  │  Move input CSV → archive directory with timestamp
  │
  ▼
END
```

### Reconciliation Logic

1. Read a transaction from CSV
2. Look up ledger entry by `externalId` → `reference`
3. **MATCHED** — amounts within $0.01 tolerance ✅
4. **DISCREPANCY** — amounts differ beyond tolerance ⚠️
5. **MISSING_IN_LEDGER** — no ledger entry found 🚨

All results are persisted with the `jobExecutionId` for full auditability.

### CSV Format

```csv
external_id,source,amount,currency,transaction_date,description
TXN-001,STRIPE,1500.0000,USD,2024-01-15,Payment for order #1001
```

---

## 🧱 SOLID Principles Applied

| Principle | Where Applied                                                                                                                                              |
| --------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **SRP**   | `ReconcileTransactionUseCase` — reconciliation logic only. `TransactionCsvMapper` — CSV parsing only. `ArchiveFileTasklet` — archiving only.               |
| **OCP**   | `ReconciliationResult` factory methods — new result types without modifying consumers. Exception hierarchy — new exceptions without changing catch blocks. |
| **LSP**   | Repository adapters implement domain port interfaces and are fully substitutable in tests.                                                                 |
| **ISP**   | `LedgerRepository` only exposes `findByReference()` — not the full CRUD that JPA provides.                                                                 |
| **DIP**   | `ReconcileTransactionUseCase` depends on `LedgerRepository` (interface), never on JPA.                                                                     |

---

## ☕ Java 21 Features Used

| Feature                                     | Where                                                                                              |
| ------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| **Records**                                 | `Transaction`, `LedgerEntry`, `ReconciliationResult`, `BatchProperties` — immutable domain objects |
| **Compact constructors**                    | `Transaction` record — validation in the constructor with no boilerplate                           |
| **Switch expressions**                      | `ArchiveFileTasklet.buildArchivedFileName()` — exhaustive, expression-style switch                 |
| **`@ConfigurationProperties` with records** | `BatchProperties` — Spring Boot 3 native support                                                   |

---

## 🗺 Development Roadmap

### ✅ Phase 1 — Foundation (Current)

- [x] Hexagonal architecture
- [x] Chunk-oriented processing (Reader → Processor → Writer)
- [x] Tasklet step (archive file)
- [x] Spring Batch 5 job/step configuration
- [x] Flyway schema migrations
- [x] Domain model as Java 21 records
- [x] Multi-stage Dockerfile
- [x] GitHub Actions CI pipeline
- [x] Zero hardcoded credentials

### 🚧 Phase 2 — Resilience & Logic (Upcoming)

- [ ] JobParameters for dynamic runtime configuration
- [ ] ExecutionContext for cross-step data sharing
- [ ] JobExecutionListener / StepExecutionListener / ChunkListener
- [ ] Conditional step flow (success → archive, failure → alert)
- [ ] Skip & retry logic for bad CSV rows
- [ ] **Redis deduplication** — prevent double-processing the same transaction
- [ ] Redis distributed lock — prevent parallel job instances

### 🔮 Phase 3 — Scale & Performance (Planned)

- [ ] Multi-threaded steps (parallel chunk processing)
- [ ] Partitioned steps (master/worker pattern for millions of records)
- [ ] Custom JPA streaming reader (cursor-based, memory-efficient)
- [ ] Performance tuning (chunk size, fetch size, transaction boundaries)
- [ ] Prometheus metrics + Grafana dashboard

---

## 🔄 CI/CD

GitHub Actions pipeline runs on every push to `main` or `develop`:

1. **Build & Test** — compiles with Java 21, runs tests against real PostgreSQL + Redis (via service containers)
2. **Code Quality** — dependency analysis
3. **Docker Build** — builds the multi-stage Docker image (push to Docker Hub is configurable)

Secrets are injected as GitHub Actions environment variables — never stored in code.
