package com.github.brane08.pagila.models

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "country")
class Country : BaseModel() {
	@Id
	@Column(name = "country_id")
	var countryId: Int? = null

	@Column(name = "country")
	var country: String? = null
}