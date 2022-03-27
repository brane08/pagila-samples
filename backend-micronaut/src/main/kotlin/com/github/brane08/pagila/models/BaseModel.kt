package com.github.brane08.pagila.models

import java.time.Instant
import javax.persistence.Column
import javax.persistence.MappedSuperclass

@MappedSuperclass
open class BaseModel {
	@Column(name = "last_update", updatable = false, insertable = false)
	var lastUpdate: Instant? = null
}