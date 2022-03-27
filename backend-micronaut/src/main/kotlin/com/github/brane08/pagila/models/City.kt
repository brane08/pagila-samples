package com.github.brane08.pagila.models

import javax.persistence.*

@Entity
@Table(name = "city")
class City : BaseModel() {
	@Id
	@Column(name = "city_id")
	var cityId: Int? = null

	@Column(name = "city")
	var city: String? = null

	@ManyToOne(optional = false)
	@JoinColumn(name = "country_id", columnDefinition = "int2")
	var country: Country? = null
}