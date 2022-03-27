from typing import List

from pymongo.database import Database

from mongo.schema import Movie


def get_movies(db: Database, page: int, size: int) -> List[Movie]:
    movies = list()
    for mv in db["movies"].find().skip(((page - 1) * size)).limit(size):
        movies.append(Movie(**mv))
    return movies
