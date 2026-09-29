-- Spring AI pgvector store (only applied when the "rag" profile adds this location).
-- Matches PgVectorStore defaults: table vector_store, cosine distance, HNSW index.

create extension if not exists vector;

create table if not exists vector_store (
    id        uuid default gen_random_uuid() primary key,
    content   text,
    metadata  json,
    embedding vector(${vector_dimensions})
);

create index if not exists vector_store_embedding_hnsw on vector_store using hnsw (embedding vector_cosine_ops);
