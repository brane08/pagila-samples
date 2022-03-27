package com.github.brane08.pagila.models

import com.vladmihalcea.hibernate.type.array.ListArrayType
import com.vladmihalcea.hibernate.type.basic.YearType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import java.time.Year
import javax.persistence.*

@Entity
@TypeDefs(
	TypeDef(name = "list-array", typeClass = ListArrayType::class),
	TypeDef(typeClass = YearType::class, defaultForType = Year::class)
)
class Film : BaseModel() {
	@Id
	@Column(name = "film_id")
	var filmId: Int? = null

	@Column(name = "title")
	var title: String? = null

	@Column(name = "description")
	var description: String? = null

	@Column(name = "release_year", columnDefinition = "int2")
	var releaseYear: Year? = null

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "language_id", columnDefinition = "int2")
	var language: Language? = null

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "original_language_id", columnDefinition = "int2")
	var originalLanguage: Language? = null

	@Column(name = "rental_duration", columnDefinition = "int2")
	var rentalDuration: Int? = null

	@Column(name = "rental_rate")
	var rentalRate: Double? = null

	@Column(name = "length", columnDefinition = "int2")
	var length: Int? = null

	@Column(name = "replacement_cost")
	var replacementCost: Double? = null

	@Column(name = "rating")
	var rating: String? = null

	@Type(type = "list-array")
	@Column(name = "special_features")
	var specialFeatures: List<String> = mutableListOf()

	@OneToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "film_category",
		joinColumns = [JoinColumn(name = "film_id", referencedColumnName = "film_id")],
		inverseJoinColumns = [JoinColumn(name = "category_id", referencedColumnName = "category_id")]
	)
	var categories: List<Category> = mutableListOf()

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "film_actor",
		joinColumns = [JoinColumn(name = "film_id", referencedColumnName = "film_id")],
		inverseJoinColumns = [JoinColumn(name = "actor_id", referencedColumnName = "actor_id")]
	)
	var actors: List<Actor> = mutableListOf()

	@Transient
	var fulltext: String? = null
}