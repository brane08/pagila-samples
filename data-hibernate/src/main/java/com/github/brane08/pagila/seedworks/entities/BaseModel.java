package com.github.brane08.pagila.seedworks.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.io.Serializable;
import java.time.Instant;

@MappedSuperclass
public class BaseModel implements Serializable {
    @Column(name = "last_update", updatable = false, insertable = false)
    Instant lastUpdate;

    public Instant getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Instant lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
