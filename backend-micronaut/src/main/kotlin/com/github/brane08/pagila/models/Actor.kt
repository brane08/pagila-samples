package com.github.brane08.pagila.models

import java.util.*
import javax.persistence.*

@Entity
@Table(name = "actor")
class Actor : BaseModel() {
	@Id
	@Column(name = "actor_id")
	var actorId: Int? = null

	@Column(name = "first_name")
	var firstName: String? = null

	@Column(name = "last_name")
	var lastName: String? = null
}