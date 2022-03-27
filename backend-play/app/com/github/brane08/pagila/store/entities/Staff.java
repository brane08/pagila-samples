package com.github.brane08.pagila.store.entities;

import com.github.brane08.pagila.seedworks.entities.BaseModel;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "staff")
public class Staff extends BaseModel {

	@Id
	@Column(name = "staff_id")
	public Integer staffId;

	@Column(name = "first_name")
	public String firstName;

	@Column(name = "last_name")
	public String lastName;

	@Column(name = "email")
	public String email;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "store_id")
	public Store store;

	@Column(name = "active")
	public boolean active;

	@Column(name = "username")
	public String username;

	@Column(name = "password")
	public String password;

	@Column(name = "picture")
	public byte[] picture;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Staff that = (Staff) o;
		return staffId.equals(that.staffId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(staffId);
	}
}
