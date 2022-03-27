package com.github.brane08.pagila.models

import javax.persistence.*

@Entity
@Table(name = "store")
class Store : BaseModel() {
	@Id
	@Column(name = "store_id")
	var storeId: Int? = null

	@ManyToOne
	@JoinColumn(name = "manager_staff_id")
	var manager: Staff? = null

	@OneToOne(optional = false)
	@JoinColumn(name = "address_id")
	var address: Address? = null

	@OneToMany(mappedBy = "store")
	var currentStaff: List<Staff> = mutableListOf()
}