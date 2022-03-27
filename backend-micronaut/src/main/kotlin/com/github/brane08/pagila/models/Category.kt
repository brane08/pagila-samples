package com.github.brane08.pagila.models

import javax.persistence.*

@Entity
@Table(name = "category")
class Category : BaseModel() {
	@Id
	@Column(name = "category_id")
	var categoryId: Int? = null

	@Column(name = "name")
	var name: String? = null
}