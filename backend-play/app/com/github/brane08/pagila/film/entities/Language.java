package com.github.brane08.pagila.film.entities;

import com.github.brane08.pagila.seedworks.entities.BaseModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "language")
public class Language extends BaseModel {
	@Id
	@Column(name = "language_id")
	public Integer languageId;

	@Column(name = "name")
	public String name;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Language that = (Language) o;
		return Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
}
