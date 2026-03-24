# 🏦 Transaction Reconciliation Engine

> A production-grade financial transaction reconciliation system built with **Spring Boot 3**, **Spring Batch 5**, **Java 21**, **PostgreSQL**, and **Redis**.
> Designed to process millions of records reliably, with full restart/recovery, Redis deduplication, skip/retry fault tolerance, and cloud-native deployment.

[![CI Pipeline](https://github.com/ItsWillBill/reconciliation-engine/actions/workflows/ci.yml/badge.svg)](https://github.com/ItsWillBill/reconciliation-engine/actions)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)](https://spring.io/projects/spring-boot)
[![Spring Batch](https://img.shields.io/badge/Spring%20Batch-5.x-green)](https://spring.io/projects/spring-batch)

---

## 📋 Table of Contents

- [Business Case](#-business-case)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [How to Run](#-how-to-run)
- [Configuration & Secrets](#-configuration--secrets)
- [How It Works](#-how-it-works)
- [SOLID Principles Applied](#-solid-principles-applied)
- [Java 21 Features Used](#-java-21-features-used)
- [Development Roadmap](#-development-roadmap)
- [CI/CD & Code Quality](#-cicd--code-quality)

---

## 💼 Business Case

Every financial institution — banks, fintechs, payment processors, e-commerce platforms — must **reconcile transactions daily**. The process answers one question:

> _Does what the bank says happened match what our internal ledger says happened?_

**Unreconciled transactions mean:**

- Lost revenue
- Regulatory non-compliance (SOX, PSD2)
- Failed audits
- Fraud going undetected

This engine automates that process at scale. It ingests raw transaction files (CSV), matches them against internal ledger entries, classifies each outcome (`MATCHED` / `DISCREPANCY` / `MISSING_IN_LEDGER`), and produces a fully auditable result set — all as a resumable, fault-tolerant, Redis-deduplicated batch job.

**Target clients:** Fintechs, payment processors, banks, insurance companies, SaaS billing platforms.

---

## 🏗 Architecture

**Why Hexagonal?** The domain never imports Spring, JPA, or Redis. The same `ReconcileTransactionUseCase` is testable in pure Java with a mocked `LedgerRepository` — no Spring context, no database, no Redis. Swapping PostgreSQL for another store requires only a new adapter, not a domain change.

---

## 🛠 Tech Stack

| Concern           | Technology                  | Version |
| ----------------- | --------------------------- | ------- |
| Language          | Java                        | 21      |
| Framework         | Spring Boot                 | 3.3.x   |
| Batch Processing  | Spring Batch                | 5.x     |
| Persistence       | Spring Data JPA + Hibernate | 6.x     |
| Database          | PostgreSQL                  | 16      |
| Cache / Dedup     | Redis (Lettuce client)      | 7.2     |
| Schema Migrations | Flyway                      | 10.x    |
| Code Quality      | SonarCloud + JaCoCo         | —       |
| Containerization  | Docker + Docker Compose     | —       |
| CI/CD             | GitHub Actions              | —       |
| Testing           | JUnit 5 + Mockito           | —       |
| Build             | Maven                       | 3.9+    |

---

## 🚀 How to Run

### ⚡ Quickstart with Docker (recommended)

**Step 1 — Configure credentials**

```bash
cp .env.example .env
# Edit .env:
#   POSTGRES_DB=reconciliation_db
#   POSTGRES_USER=recon_user
#   POSTGRES_PASSWORD=yourpassword
#   REDIS_PASSWORD=          ← leave blank for no auth locally
```

**Step 2 — Copy a CSV file into the input directory**

```bash
cp data/input/samples/sample_transactions.csv data/input/
```

> ⚠️ **Why this step?** After the job runs, the processed CSV is moved to `data/archive/`
> with a timestamp. The `input/` folder is intentionally empty between runs.
> Always copy fresh from `samples/` or drop your own file before each run.

**Step 3 — Run**

```bash
# Default — processes sample_transactions.csv, runDate = today
docker-compose up --build

# Specific file
INPUT_FILE=my_transactions.csv docker-compose up --build

# Specific file + explicit runDate (for backfill / correction runs)
INPUT_FILE=corrections.csv RUN_DATE=2024-01-10 docker-compose up --build
```

**Step 4 — Run again**

```bash
# App exits after job completes (restart: no — batch jobs run once).
cp data/input/samples/sample_transactions.csv data/input/my_run2.csv
INPUT_FILE=my_run2.csv docker-compose up
```

---

### 💻 Run Locally (without Docker)

```bash
# Start only infrastructure
docker-compose up postgres redis

# Copy sample file
cp data/input/samples/sample_transactions.csv data/input/

# Run — Spring Boot's JobLauncherApplicationRunner picks up the key=value arg
export SPRING_PROFILES_ACTIVE=local
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="inputFileName=sample_transactions.csv"

# With explicit runDate
./mvnw spring-boot.run \
  -Dspring-boot.run.arguments="inputFileName=sample_transactions.csv runDate=2024-01-10"
```

---

### 🔄 Clean Slate

```bash
docker-compose down -v                                            # wipes DB + volumes
cp data/input/samples/sample_transactions.csv data/input/
docker-compose up --build
```

---

## 🔐 Configuration & Secrets

Zero credentials are hardcoded. All sensitive values come from environment variables.

| Variable            | Description                        | Default                   |
| ------------------- | ---------------------------------- | ------------------------- |
| `POSTGRES_HOST`     | PostgreSQL hostname                | `localhost`               |
| `POSTGRES_PORT`     | PostgreSQL port                    | `5432`                    |
| `POSTGRES_DB`       | Database name                      | _(required)_              |
| `POSTGRES_USER`     | Database username                  | _(required)_              |
| `POSTGRES_PASSWORD` | Database password                  | _(required)_              |
| `REDIS_HOST`        | Redis hostname                     | `localhost`               |
| `REDIS_PORT`        | Redis port                         | `6379`                    |
| `REDIS_PASSWORD`    | Redis password (empty = no auth)   | `""`                      |
| `APP_INPUT_DIR`     | CSV input directory                | `/data/input`             |
| `APP_ARCHIVE_DIR`   | Archive directory                  | `/data/archive`           |
| `BATCH_CHUNK_SIZE`  | Records per chunk (10–900)         | `500`                     |
| `BATCH_SKIP_LIMIT`  | Max skippable CSV rows before fail | `10`                      |
| `BATCH_RETRY_LIMIT` | Max retries on transient errors    | `3`                       |
| `BATCH_DEDUP_TTL`   | Redis dedup key TTL in days        | `7`                       |
| `INPUT_FILE`        | CSV filename (Docker Compose only) | `sample_transactions.csv` |

---

## ⚙️ How It Works

### Batch Job Flow

```
START
  │
  ▼
[reconciliationStep] — chunk-oriented, fault-tolerant
  │  1. Read CSV row → Transaction (via @StepScope FlatFileItemReader)
  │  2. Processor:
  │     a. Redis dedup check → if duplicate, filter (return null)
  │     b. ReconcileTransactionUseCase → ReconciliationResult
  │     c. Mark processed in Redis (TTL-scoped to runDate)
  │  3. Writer (3 SQL per chunk):
  │     → bulk SELECT existing transactions
  │     → bulk INSERT new transactions
  │     → bulk INSERT reconciliation results
  │  4. Skip: CsvParsingException → skipped_items table, continue
  │  5. Retry: TransientReconciliationException → up to 3 retries
  │
  ├── on COMPLETED ──► [archiveFileStep]
  │                      Move CSV → archive/ with timestamp
  │                      → END (COMPLETED)
  │
  └── on FAILED ──────► [reportFailureStep]
                          Log structured failure summary
                          → END (FAILED)
```

### Reconciliation Logic

| Case                                            | Outcome                |
| ----------------------------------------------- | ---------------------- |
| Ledger entry found, amounts match within $0.01  | `MATCHED` ✅           |
| Ledger entry found, amounts differ beyond $0.01 | `DISCREPANCY` ⚠️       |
| No ledger entry found for this reference        | `MISSING_IN_LEDGER` 🚨 |

### Redis Deduplication

```
Key:   recon:dedup:{externalId}:{runDate}
Value: "1"
TTL:   configurable (default 7 days)

On processor entry:
  Redis GET key → exists? → return null (filtered, not skipped)
  Redis GET key → missing? → reconcile → Redis SET key with TTL
```

Redis is an optimisation, not a hard dependency. If Redis is unavailable, the processor logs a warning and continues — the DB `UNIQUE` constraint on `external_id` remains the safety net.

### Skip & Retry Policy

| Exception                          | Action                    | Reason                                        |
| ---------------------------------- | ------------------------- | --------------------------------------------- |
| `CsvParsingException`              | Skip (up to skip-limit)   | Bad CSV row — will never improve on retry     |
| `TransientReconciliationException` | Retry (up to retry-limit) | DB timeout, Redis blip — may succeed on retry |
| `IllegalStateException`            | Fail job                  | Bug in our code — fail fast                   |
| Any other                          | Fail job                  | Unknown — investigate before retrying         |

### CSV Format

```csv
external_id,source,amount,currency,transaction_date,description
TXN-001,STRIPE,1500.0000,USD,2024-01-15,Payment for order #1001
```

---

## 🧱 SOLID Principles Applied

| Principle | Where                                                                                                                                                                                                          |
| --------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **SRP**   | `ReconcileTransactionUseCase` — reconciliation only. `TransactionCsvMapper` — parsing only. `DeduplicationService` — dedup only. `SkipItemListener` — skip persistence only. Each listener covers one concern. |
| **OCP**   | `ReconciliationResult` factory methods — new result types without modifying consumers. `ReconciliationSkipPolicy` — new skippable exceptions added without touching step config.                               |
| **LSP**   | Repository adapters implement domain ports and are fully substitutable in tests.                                                                                                                               |
| **ISP**   | `LedgerRepository` exposes only `findByReference()`. `TransactionRepository` exposes only what reconciliation needs.                                                                                           |
| **DIP**   | `ReconcileTransactionUseCase` depends on `LedgerRepository` (interface), never on JPA. `ReconciliationItemProcessor` depends on `DeduplicationService` (concrete but injected).                                |

---

## ☕ Java 21 Features Used

| Feature                           | Where                                                                                              |
| --------------------------------- | -------------------------------------------------------------------------------------------------- |
| **Records**                       | `Transaction`, `LedgerEntry`, `ReconciliationResult`, `BatchProperties` — immutable domain objects |
| **Compact constructors**          | `Transaction` — domain validation with no boilerplate                                              |
| **Switch expressions**            | `ArchiveFileTasklet`, `JobExecutionReportListener` — exhaustive, expression-style                  |
| **Pattern matching `instanceof`** | `ReconciliationSkipPolicy` — `if (t instanceof CsvParsingException csvEx)`                         |
| **`List.toList()`**               | Writer — immutable list from stream (Java 16+)                                                     |
| **Text blocks**                   | `SkipItemListener` — multi-line SQL                                                                |

---

## 🗺 Development Roadmap

### ✅ Phase 1 — Foundation

- [x] Hexagonal architecture with domain, application, and infrastructure layers
- [x] Chunk-oriented processing (FlatFileItemReader → Processor → ItemWriter)
- [x] Archive tasklet step
- [x] Spring Batch 5 job/step wiring with `@StepScope`
- [x] Flyway schema migrations
- [x] Domain model as Java 21 records with compact constructor validation
- [x] Bulk writer — 3 SQL statements per chunk, concurrency-safe upsert
- [x] Multi-stage Dockerfile + Docker Compose
- [x] GitHub Actions CI pipeline
- [x] Zero hardcoded credentials

### ✅ Phase 2 — Resilience & Logic

- [x] `JobParameters` — `inputFileName` + `runDate` + `run.id`
- [x] `ReconciliationJobParametersConverter` — replaces `CommandLineRunner`, native Spring Batch integration
- [x] `JobExecutionReportListener` — structured job start/end summary
- [x] `StepProgressListener` — per-step read/write/skip/duration metrics
- [x] `ChunkMetricsListener` — per-chunk timing with slow-chunk warning
- [x] `SkipItemListener` — persists skipped rows to `skipped_items` audit table
- [x] Conditional flow — `COMPLETED → archiveStep`, `FAILED → reportFailureStep`
- [x] `ReconciliationSkipPolicy` — `CsvParsingException` skippable, `TransientReconciliationException` retryable
- [x] `DeduplicationService` — Redis TTL-scoped dedup keys, graceful Redis failure handling
- [x] `BatchProperties` — `@Validated` with bounds on all numeric fields
- [x] SonarCloud integration with JaCoCo coverage

### 🔮 Phase 3 — Scale & Performance

- [ ] Multi-threaded step — `TaskExecutor` with thread-safe reader
- [ ] Partitioned step — master/worker pattern, date-range partitioning
- [ ] Custom JPA cursor-based streaming reader for DB-sourced input
- [ ] Performance tuning — chunk size, fetch size, transaction boundaries
- [ ] Remote chunking concept — Kafka-based distribution outline
- [ ] Prometheus metrics endpoint + Grafana dashboard

---

## 🔄 CI/CD & Code Quality

### Pipeline

| Trigger                   | Build & Test | SonarCloud | Docker |
| ------------------------- | :----------: | :--------: | :----: |
| Push `feature/*`, `fix/*` |      ✅      |     ⏭     |   ⏭   |
| Push `develop`            |      ✅      |     ✅     |   ⏭   |
| Push `main`               |      ✅      |     ✅     |   ✅   |
| PR → `develop`            |      ✅      |     ✅     |   ⏭   |
| PR → `main`               |      ✅      |     ✅     |   ✅   |

### SonarCloud Setup (one-time)

```bash
# 1. https://sonarcloud.io → login with GitHub → import repo
# 2. Update sonar.projectKey and sonar.organization in pom.xml
# 3. Generate token: SonarCloud → My Account → Security
# 4. GitHub → Settings → Secrets → Actions → New secret: SONAR_TOKEN
```

### Run Tests Locally

```bash
./mvnw verify                    # tests + JaCoCo coverage report
./mvnw sonar:sonar               # push to SonarCloud (requires SONAR_TOKEN)
```

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.
