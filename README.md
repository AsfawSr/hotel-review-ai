# HotelReviewAI

AI-powered Hotel Guest Review Analyzer System built with Spring Boot 3, Spring AI, and PostgreSQL.

## Quick Start (Local)

1. Start PostgreSQL with pgvector enabled.
2. Start Ollama and pull the required models.
3. Run the application.

## Common Environment Variables

- `DB_URL` (e.g., `jdbc:postgresql://localhost:5432/hotel_review_ai`)
- `DB_USERNAME`
- `DB_PASSWORD`
- `OLLAMA_BASE_URL` (default: `http://localhost:11434`)
- `OLLAMA_CHAT_MODEL` (default: `llama3.2`)
- `OLLAMA_EMBEDDING_MODEL` (default: `nomic-embed-text`)
- `APP_ADMIN_USERNAME` (default: `admin`)
- `APP_ADMIN_PASSWORD` (default: `admin123`)

## Build

```powershell
mvnw.cmd -q test
```

## Run

```powershell
mvnw.cmd spring-boot:run
```

## RAG Setup Note

Pgvector auto-configuration is disabled by default to prevent startup failures when the extension is not installed. Enable it by removing the exclude in `src/main/resources/application.yaml` and setting `app.rag.enabled=true`.

## Web UI

- Dashboard: `http://localhost:8080/dashboard`
- Reviews: `http://localhost:8080/reviews`
- Submit review: `http://localhost:8080/reviews/submit`

Reviews run AI analysis automatically when the chat model is configured. If AI is disabled, the review is still saved and marked as Pending.
