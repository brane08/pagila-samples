package com.github.brane08.pagila.seedworks.entities;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.time.Instant;

@MappedSuperclass
public class BaseModel implements Serializable {
	@Column(name = "last_update", updatable = false, insertable = false)
	public Instant lastUpdate;
}
