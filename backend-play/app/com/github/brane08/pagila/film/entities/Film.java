package com.github.brane08.pagila.film.entities;

import com.github.brane08.pagila.seedworks.entities.BaseModel;
import com.vladmihalcea.hibernate.type.array.ListArrayType;
import com.vladmihalcea.hibernate.type.basic.YearType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "film")
@TypeDefs({
		@TypeDef(name = "list-array", typeClass = ListArrayType.class),
		@TypeDef(typeClass = YearType.class, defaultForType = Year.class)
})
public class Film extends BaseModel {

	@Id
	@Column(name = "film_id")
	public Integer filmId;

	@Column(name = "title")
	public String title;

	@Column(name = "description")
	public String description;

	@Column(name = "release_year", columnDefinition = "int2")
	public Year releaseYear;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "language_id", referencedColumnName = "language_id", columnDefinition = "int2")
	public Language language;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "original_language_id", referencedColumnName = "language_id", columnDefinition = "int2")
	public Language originalLanguage;

	@Column(name = "rental_duration")
	public Long rentalDuration;

	@Column(name = "rental_rate")
	public Double rentalRate;

	@Column(name = "length", columnDefinition = "int2")
	public Integer length;

	@Column(name = "replacement_cost")
	public Double replacementCost;

	@Column(name = "rating")
	public String rating;

	@Type(type = "list-array")
	@Column(name = "special_features")
	public List<String> specialFeatures = new ArrayList<>();

	@ManyToMany
	@JoinTable(name = "film_category",
			joinColumns = @JoinColumn(name = "film_id", referencedColumnName = "film_id"),
			inverseJoinColumns = @JoinColumn(name = "category_id", referencedColumnName = "category_id")
	)
	public List<Category> categories;
}
