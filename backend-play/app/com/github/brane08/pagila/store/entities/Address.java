package com.github.brane08.pagila.store.entities;

import com.github.brane08.pagila.seedworks.entities.BaseModel;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "address")
public class Address extends BaseModel {
	@Id
	@Column(name = "address_id")
	public Integer addressId;

	@Column(name = "address")
	public String address;

	@Column(name = "address2")
	public String address2;

	@Column(name = "district")
	public String district;

	@Column(name = "postal_code")
	public String postalCode;

	@Column(name = "phone")
	public String phone;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "city_id", columnDefinition = "int2")
	public City city;

	@OneToOne(mappedBy = "address")
	public Store store;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Address that = (Address) o;
		return addressId.equals(that.addressId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(addressId);
	}
}
