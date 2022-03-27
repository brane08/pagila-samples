import os

from pymongo import MongoClient
from pymongo.database import Database


class MongoApi:
    __client: MongoClient = None
    __database: Database = None

    @classmethod
    def init_db(cls):
        cls.__client = MongoClient(os.environ.get("APP_MONGO_URL"))
        cls.__database: Database = cls.__client["sample_mflix"]
        print("I am connected!")

    @classmethod
    def client(cls) -> MongoClient:
        return cls.__client

    @classmethod
    def database(cls) -> Database:
        return cls.__database

    @classmethod
    def close(cls):
        if cls.__client is not None:
            cls.__client.close()
