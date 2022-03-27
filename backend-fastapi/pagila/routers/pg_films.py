from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from dependencies import get_db
from responses import ApiResponse
from sql import operations, models
from sql.schema import Film

router = APIRouter(prefix="/pg/films", tags=["films"])


@router.get("")
def get_films(db: Session = Depends(get_db), page: int = 1, size: int = 20):
    actors = operations.get_films(db, page, size)
    film_list = list()
    for act in actors:
        film_list.append(sql_to_schema(act))
    return ApiResponse.respond_paged(page, size, -1, list(film_list))


def sql_to_schema(film: models.Film):
    return Film(id=film.id, title=film.title, last_update=film.last_update, length=film.length,
                description=film.description, release_year=film.release_year, rental_duration=film.rental_duration,
                rental_rate=film.rental_rate, replacement_cost=film.replacement_cost)
