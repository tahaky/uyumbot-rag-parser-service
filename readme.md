# Chunking Service (Java / Spring Boot)

Production-ready **Chunking Service** for RAG (Retrieval-Augmented Generation) systems.
Java/Spring Boot re-implementation of the [Python chunking-service](https://github.com/tahaky/chunking-service)
with **direct CRUD REST API** — no Kafka required.

## Features

- **Format-Specific Chunking**
  - **DOCX** – section-based chunking with hierarchical stable IDs
  - **PDF** – page-based chunking
  - **PPTX** – slide-based chunking
  - **XLSX** – sheet-based chunking
- **Stable Chunk IDs** – prevents butterfly effect (changing one section does not affect others)
- **Change Detection** – MD5 hashing; only changed chunks are updated
- **Direct CRUD REST API** – no Kafka, straightforward HTTP endpoints
- **PostgreSQL** persistence via Spring Data JPA
- **Health Check** – Spring Actuator at `/actuator/health`
- **Docker support** – Dockerfile + docker-compose.yml

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Database | PostgreSQL 15 + Spring Data JPA |
| Build | Maven |
| Container | Docker / docker-compose |

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 15+ (or Docker)

### Run with Docker Compose

```bash
docker-compose up -d
```

### Run locally

```bash
# Start PostgreSQL
docker run -d -p 5432:5432 \
  -e POSTGRES_DB=rag \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  postgres:15-alpine

# Build and run
mvn spring-boot:run
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/rag` | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | `postgres` | DB username |
| `DATABASE_PASSWORD` | `postgres` | DB password |
| `SERVER_PORT` | `8080` | HTTP port |
| `MAX_CHUNK_SIZE` | `800` | Max characters per chunk |

## API Reference

### Documents

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/documents` | List all documents |
| `POST` | `/api/documents` | Create document (optional auto-chunk) |
| `GET` | `/api/documents/{id}` | Get document by ID |
| `PUT` | `/api/documents/{id}` | Update document (optional re-chunk) |
| `DELETE` | `/api/documents/{id}` | Delete document and its chunks |
| `POST` | `/api/documents/{id}/chunk` | Trigger (re-)chunking with structure |

### Chunks

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/documents/{docId}/chunks` | List chunks for a document |
| `POST` | `/api/documents/{docId}/chunks` | Manually add a chunk |
| `DELETE` | `/api/documents/{docId}/chunks` | Delete all chunks for document |
| `GET` | `/api/chunks/{id}` | Get chunk by ID |
| `PUT` | `/api/chunks/{id}` | Update chunk |
| `DELETE` | `/api/chunks/{id}` | Delete chunk |

## Example Requests

### Create and auto-chunk a DOCX document

```json
POST /api/documents
{
  "filename": "report.docx",
  "format": "docx",
  "structure": {
    "doc_id": "550e8400-e29b-41d4-a716-446655440000",
    "sections": [
      {
        "level": 1,
        "title": "Introduction",
        "paragraphs": [
          {"style": "Normal", "text": "This document presents..."}
        ],
        "tables": []
      }
    ]
  }
}
```

### Chunk a PDF document

```json
POST /api/documents/{id}/chunk
{
  "doc_id": "550e8400-e29b-41d4-a716-446655440000",
  "pages": [
    {"page_number": 1, "text": "Page one content...", "tables": []},
    {"page_number": 2, "text": "Page two content...", "tables": []}
  ]
}
```

## Database Schema

```sql
CREATE TABLE documents (
    id          UUID PRIMARY KEY,
    filename    VARCHAR(512),
    format      VARCHAR(50),
    status      VARCHAR(50),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE TABLE chunks (
    chunk_id    UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    stable_id   VARCHAR(255) NOT NULL,
    text        TEXT NOT NULL,
    chunk_type  VARCHAR(50) NOT NULL,
    hash        VARCHAR(64) NOT NULL,
    metadata    JSONB,
    word_count  INTEGER,
    char_count  INTEGER,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    UNIQUE (document_id, stable_id)
);
```

## Running Tests

```bash
mvn test
```
