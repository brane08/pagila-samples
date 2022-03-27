from typing import Optional

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from dependencies import get_db
from responses import ApiResponse
from sql import operations, models
from sql.models import Actor

router = APIRouter(prefix="/pg/actors", tags=["actors"], )


@router.get("", response_model_exclude_none=True, response_model=dict)
def get_actors(db: Session = Depends(get_db), page: int = 1, size: int = 20):
    actors = operations.get_actors(db, page, size)
    actor_list = list()
    for act in actors:
        actor_list.append(sql_to_schema(act))
    return ApiResponse.respond_paged(page, size, -1, list(actor_list))


@router.get("/{actor_id}", response_model_exclude_none=True)
def get_actor(actor_id: int, db: Session = Depends(get_db)):
    return ApiResponse.respond(sql_to_schema(operations.get_actor(db, actor_id)))


def sql_to_schema(actor: models.Actor):
    return Actor(id=actor.id, first_name=actor.first_name, last_name=actor.last_name,
                 last_update=actor.last_update)
