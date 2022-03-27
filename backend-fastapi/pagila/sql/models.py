from sqlalchemy import Column, Integer, String, DateTime, Float, Boolean
from sqlalchemy.orm import declarative_base

Base = declarative_base()


class Actor(Base):
    __tablename__ = "actor"

    id = Column(Integer, primary_key=True, index=True, name="actor_id")
    first_name = Column(String, unique=True, index=True, name="first_name")
    last_name = Column(String, name="last_name")
    last_update = Column(DateTime, default=True, name="last_update")


class Customer(Base):
    __tablename__ = "customer"

    id = Column(Integer, primary_key=True, index=True, name="customer_id")
    first_name = Column(String, unique=True, index=True, name="first_name")
    last_name = Column(String, name="last_name")
    email = Column(String, name="email")
    activebool = Column(Boolean, name="activebool")
    active = Column(Boolean, name="active")
    create_date = Column(DateTime, name="create_date")
    last_update = Column(DateTime, default=True, name="last_update")


class User(Base):
    __tablename__ = "users_t"

    id = Column(Integer, primary_key=True, index=True, name="user_id", autoincrement=True)
    user_name = Column(String, unique=True, index=True, name="user_name")
    password = Column(String, name="password_hash")


class Film(Base):
    __tablename__ = "film"

    id = Column(Integer, primary_key=True, index=True, name="film_id")
    title = Column(String, unique=True, index=True, name="title")
    description = Column(String, name="description")
    release_year = Column(Integer, name="release_year")
    rental_duration = Column(Integer, name="rental_duration")
    rental_rate = Column(Float, name="rental_rate")
    length = Column(Integer, default=True, name="length")
    replacement_cost = Column(Float, default=True, name="replacement_cost")
    last_update = Column(DateTime, default=True, name="last_update")
