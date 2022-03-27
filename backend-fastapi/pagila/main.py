import calendar
import multiprocessing
import os
import time
from pathlib import Path

import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from mongo.client import MongoApi
from processes.main import ProcessPool
from routers import pg_actors, mg_movies, pg_users, pg_films
from sql.database import DatabaseApi
from threads.main import ThreadPool

app = FastAPI()

origins = ["http://localhost", "https://localhost:4200", "http://localhost:8000"]

app.add_middleware(CORSMiddleware, allow_origins=origins, allow_credentials=True,
                   allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", ],
                   allow_headers=["*"], )

app.include_router(pg_actors.router)
app.include_router(pg_films.router)
app.include_router(mg_movies.router)
app.include_router(pg_users.router)


@app.get("/")
async def root(workers: int = 10):
    ProcessPool.submit(workers)
    return {"message": "Hello World"}


@app.get("/futures", response_model=dict)
async def updates():
    return ProcessPool.get_status()


@app.post("/th")
async def root(workers: int = 10):
    ThreadPool.submit(workers)
    return {"message": "Hello World"}


@app.get("/th/futures", response_model=dict)
async def updates():
    return ThreadPool.get_status()


@app.on_event("startup")
def startup_event() -> None:
    DatabaseApi.init_db()
    MongoApi.init_db()
    # ProcessPool.setup_pool(min((multiprocessing.cpu_count() * 2), 8))
    # ThreadPool.setup_pool(min((multiprocessing.cpu_count() * 8), 32))


@app.on_event("shutdown")
def shutdown_event():
    DatabaseApi.close()
    MongoApi.close()
    # ProcessPool.shutdown()
    # ThreadPool.shutdown()


def get_log_folder() -> Path:
    utc_now = time.gmtime()
    hour = int(int((utc_now.tm_hour / 8)) * 8)
    final_struct = (utc_now.tm_year, utc_now.tm_mon, utc_now.tm_mday, hour, 0, 0, 0, 0, 0)
    hour_folder = str(calendar.timegm(time.struct_time(final_struct)))
    return Path(Path(os.getcwd()).parent, "logs", hour_folder).resolve()


if __name__ == "__main__":
    log_dir = get_log_folder()
    if not log_dir.exists():
        log_dir.mkdir(parents=True)
    uvicorn.run("__main__:app", host="0.0.0.0", port=8000, reload=True)
