from datetime import datetime
from typing import Optional

from pydantic import BaseModel


class Actor(BaseModel):
    id: int
    first_name: str
    last_name: str
    last_update: datetime

    class Config:
        orm_mode = True


class Film(BaseModel):
    id: int
    title: str
    description: str
    release_year: int
    rental_duration: int
    rental_rate: float
    length: int
    replacement_cost: float
    last_update: datetime

    class Config:
        orm_mode = True


class UserBase(BaseModel):
    user_id: Optional[int]
    user_name: str
    password: Optional[str]


class UserCreate(UserBase):
    pass
