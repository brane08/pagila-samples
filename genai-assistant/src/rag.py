# src/rag.py
import os

import asyncpg
from dotenv import load_dotenv
from langchain_community.embeddings import FastEmbedEmbeddings
from langchain_core.documents import Document
from langchain_postgres import PGVector

load_dotenv()

# ONNX-based local embeddings — no PyTorch required, works on all platforms.
# Model is compatible with all-MiniLM-L6-v2 (same 384-dim space).
embeddings = FastEmbedEmbeddings(model_name="BAAI/bge-small-en-v1.5")

DB_CONNECTION = (
    f"postgresql+psycopg://{os.getenv('DB_USER', 'postgres')}"
    f":{os.getenv('DB_PASSWORD', 'postgres')}"
    f"@{os.getenv('DB_HOST', 'localhost')}"
    f":{os.getenv('DB_PORT', '5432')}"
    f"/{os.getenv('DB_NAME', 'sakila')}"
)

COLLECTION_NAME = "film_descriptions"

def get_vector_store() -> PGVector:
    """Return a PGVector store backed by the Pagila database."""
    return PGVector(
        embeddings=embeddings,
        collection_name=COLLECTION_NAME,
        connection=DB_CONNECTION,
        use_jsonb=True,
    )


async def index_films():
    """
    Fetch all films from Pagila and upsert their embeddings into pgvector.
    Run this once (or when film data changes).
    """
    conn = await asyncpg.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", 5432)),
        database=os.getenv("DB_NAME", "sakila"),
        user=os.getenv("DB_USER", "postgres"),
        password=os.getenv("DB_PASSWORD", "postgres"),
    )
    try:
        rows = await conn.fetch(
            """
            SELECT f.film_id, f.title, f.description,
                   f.rating::text, f.length, f.rental_rate,
                   l.name AS language,
                   STRING_AGG(DISTINCT c.name, ', ') AS categories,
                   STRING_AGG(
                           DISTINCT a.first_name || ' ' || a.last_name, ', '
                   ) AS actors
            FROM film f
                     JOIN language l ON f.language_id = l.language_id
                     LEFT JOIN film_category fc ON f.film_id = fc.film_id
                     LEFT JOIN category c ON fc.category_id = c.category_id
                     LEFT JOIN film_actor fa ON f.film_id = fa.film_id
                     LEFT JOIN actor a ON fa.actor_id = a.actor_id
            GROUP BY f.film_id, f.title, f.description,
                     f.rating, f.length, f.rental_rate, l.name
            """
        )
    finally:
        await conn.close()

    # Build LangChain Documents — content is what gets embedded
    docs = []
    for r in rows:
        content = (
            f"Title: {r['title']}\n"
            f"Description: {r['description']}\n"
            f"Categories: {r['categories'] or 'N/A'}\n"
            f"Actors: {r['actors'] or 'N/A'}\n"
            f"Rating: {r['rating']}  Length: {r['length']} min  "
            f"Rental rate: ${r['rental_rate']}"
        )
        docs.append(Document(
            page_content=content,
            metadata={
                "film_id": r["film_id"],
                "title": r["title"],
                "rating": r["rating"],
                "length": r["length"],
                "rental_rate": float(r["rental_rate"]),
                "language": r["language"],
                "categories": r["categories"] or "",
            },
        ))

    store = get_vector_store()
    store.add_documents(docs, ids=[str(d.metadata["film_id"]) for d in docs])
    print(f"Indexed {len(docs)} films into pgvector.")


async def semantic_search(query: str, k: int = 5) -> list[dict]:
    """Return k most semantically similar films to the query string."""
    store = get_vector_store()
    results = store.similarity_search_with_score(query, k=k)
    return [
        {
            **doc.metadata,
            "description_snippet": doc.page_content[:200],
            "similarity_score": round(float(score), 4),
        }
        for doc, score in results
    ]


# ── Run indexing directly ─────────────────────────────────────────────────────
if __name__ == "__main__":
    import asyncio
    asyncio.run(index_films())