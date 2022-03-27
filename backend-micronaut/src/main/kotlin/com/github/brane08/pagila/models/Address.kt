package com.github.brane08.pagila.models

import javax.persistence.*

@Entity
class Address : BaseModel() {
	@Id
	@Column(name = "address_id")
	var addressId: Int? = null

	@Column(name = "address")
	var address: String? = null

	@Column(name = "address2")
	var address2: String? = null

	@Column(name = "district")
	var district: String? = null

	@Column(name = "postal_code")
	var postalCode: String? = null

	@Column(name = "phone")
	var phone: String? = null

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "city_id", columnDefinition = "int2")
	var city: City? = null

	@OneToOne(mappedBy = "address")
	var store: Store? = null
}