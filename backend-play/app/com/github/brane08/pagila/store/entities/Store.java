package com.github.brane08.pagila.store.entities;

import com.github.brane08.pagila.seedworks.entities.BaseModel;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "store")
public class Store extends BaseModel {

	@Id
	@Column(name = "store_id")
	public Integer storeId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "manager_staff_id")
	public Staff manager;

	@OneToOne(optional = false)
	@JoinColumn(name = "address_id")
	public Address address;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Store that = (Store) o;
		return storeId.equals(that.storeId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(storeId);
	}
}
