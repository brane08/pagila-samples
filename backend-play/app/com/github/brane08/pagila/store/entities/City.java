package com.github.brane08.pagila.store.entities;

import com.github.brane08.pagila.seedworks.entities.BaseModel;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "city")
public class City extends BaseModel {
	@Id
	@Column(name = "city_id")
	public Integer cityId;

	@Column(name = "city")
	public String city;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "country_id", columnDefinition = "int2")
	public Country country;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		City that = (City) o;
		return Objects.equals(city, that.city) && Objects.equals(country, that.country);
	}

	@Override
	public int hashCode() {
		return Objects.hash(city, country);
	}
}
