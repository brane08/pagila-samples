from fastapi import APIRouter, Depends
from pymongo.database import Database

from dependencies import get_m_db
from mongo import operations
from responses import ApiResponse

router = APIRouter(prefix="/mg/movies", tags=["movies"])


@router.get("", response_model_exclude_unset=True)
def get_movies(db: Database = Depends(get_m_db), page: int = 1, size: int = 20):
    movies = operations.get_movies(db, page, size)
    return ApiResponse.respond(movies)


@router.get("/{id}")
def get_actor(id: int):
    return {"success": True, "data": []}
