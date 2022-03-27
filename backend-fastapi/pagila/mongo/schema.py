from typing import Optional

from bson import ObjectId
from pydantic import BaseModel, Field


class PyObjectId(ObjectId):

    @classmethod
    def __get_validators__(cls):
        yield cls.validate

    @classmethod
    def validate(cls, v):
        if not ObjectId.is_valid(v):
            raise ValueError('Invalid objectid')
        return ObjectId(v)

    @classmethod
    def __modify_schema__(cls, field_schema):
        field_schema.update(type='string')


class Award(BaseModel):
    wins: int
    nominations: int
    text: str

    class Config:
        arbitrary_types_allowed = True


class Movie(BaseModel):
    movie_id: PyObjectId = Field(alias="_id", title="id")
    awards: Award
    full_plot: Optional[str] = Field(None, alias="fullplot")
    last_updated: str = Field(..., alias="lastupdated")
    plot: Optional[str]
    title: str
    type: str
    year: int

    class Config:
        json_encoders = {
            ObjectId: str
        }
