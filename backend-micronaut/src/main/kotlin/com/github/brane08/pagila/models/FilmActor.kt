package com.github.brane08.pagila.models

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "film_actor")
class FilmActor : BaseModel() {
	@Id
	@Column(name = "actor_id")
	var actorId: Int? = null

	@Column(name = "film_id")
	var filmId: Int? = null
}