package com.github.brane08.pagila.actor.beans;

import java.io.Serializable;
import java.time.Instant;

public record ActorInfo(int actorId, String firstName, String lastName, Instant lastUpdate) implements Serializable {
}
