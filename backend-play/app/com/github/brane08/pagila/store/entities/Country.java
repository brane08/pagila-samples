package com.github.brane08.pagila.store.entities;

import com.github.brane08.pagila.seedworks.entities.BaseModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "country")
public class Country extends BaseModel {

	@Id
	@Column(name = "country_id")
	public Integer countryId;

	@Column(name = "country")
	public String country;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Country that = (Country) o;
		return Objects.equals(country, that.country);
	}

	@Override
	public int hashCode() {
		return Objects.hash(country);
	}
}
