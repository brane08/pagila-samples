package com.github.brane08.pagila.models

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "film_category")
class FilmCategory : BaseModel() {
	@Id
	@Column(name = "film_id")
	var filmId: Int? = null

	@Column(name = "category_id")
	var categoryId: Int? = null
}