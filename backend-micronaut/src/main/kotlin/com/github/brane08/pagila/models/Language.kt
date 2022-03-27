package com.github.brane08.pagila.models

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "language")
class Language : BaseModel() {
	@Id
	@Column(name = "language_id")
	var languageId: Int? = null

	@Column(name = "name")
	var name: String? = null
}