package com.github.brane08.pagila.actor.beans;


import java.io.Serializable;
import java.time.Instant;

public record FilmActorInfo(Long actorId, Long filmId, Instant lastUpdate) implements Serializable {
}
