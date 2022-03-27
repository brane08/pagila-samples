package com.github.brane08.pagila.film.entities;

import com.github.brane08.pagila.seedworks.entities.BaseModel;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "category")
public class Category extends BaseModel {
	@Id
	@Column(name = "category_id")
	public Integer categoryId;

	@Column(name = "name")
	public String name;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Category that = (Category) o;
		return Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
}
