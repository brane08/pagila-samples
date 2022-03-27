package com.github.brane08.pagila.models

import javax.persistence.*

@Entity
@Table(name = "staff")
class Staff : BaseModel() {
	@Id
	@Column(name = "staff_id")
	var staffId: Int? = null

	@Column(name = "first_name")
	var firstName: String? = null

	@Column(name = "last_name")
	var lastName: String? = null

	@Column(name = "email")
	var email: String? = null

	@ManyToOne(optional = false)
	@JoinColumn(name = "store_id")
	var store: Store? = null

	@Column(name = "active")
	var active = false

	@Column(name = "username")
	var username: String? = null

	@Column(name = "password")
	var password: String? = null

	@Column(name = "picture")
	var picture: ByteArray? = null
}