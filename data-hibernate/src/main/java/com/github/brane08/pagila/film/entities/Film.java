package com.github.brane08.pagila.film.entities;

import com.github.brane08.pagila.seedworks.entities.BaseModel;
import com.github.brane08.pagila.seedworks.entities.YearConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.Year;
import java.util.List;

@Entity
@Table(name = "film")
public class Film extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "film_id")
    Integer filmId;

    @Column(name = "title")
    String title;

    @Column(name = "description")
    String description;

    @Convert(converter = YearConverter.class)
    @Column(name = "release_year")
    Year releaseYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", referencedColumnName = "language_id", columnDefinition = "int2")
    Language language;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_language_id", referencedColumnName = "language_id", columnDefinition = "int2")
    Language originalLanguage;

    @Column(name = "rental_duration")
    Integer rentalDuration;

    @Column(name = "rental_rate")
    Double rentalRate;

    @Column(name = "length", columnDefinition = "int2")
    Integer length;

    @Column(name = "replacement_cost")
    Double replacementCost;

    @Column(name = "rating_txt")
    @Type(com.github.brane08.pagila.seedworks.entities.FilmRatingType.class)
    FilmRating rating;

    @Type(com.github.brane08.pagila.seedworks.entities.StringArrayType.class)
    @Column(name = "special_features", columnDefinition = "text[]")
    String[] specialFeatures;

    @ManyToMany
    @JoinTable(name = "film_category",
            joinColumns = @JoinColumn(name = "film_id", referencedColumnName = "film_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id", referencedColumnName = "category_id")
    )
    List<Category> categories;

    public Integer getFilmId() {
        return filmId;
    }

    public void setFilmId(Integer filmId) {
        this.filmId = filmId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Year getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Year releaseYear) {
        this.releaseYear = releaseYear;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Language getOriginalLanguage() {
        return originalLanguage;
    }

    public void setOriginalLanguage(Language originalLanguage) {
        this.originalLanguage = originalLanguage;
    }

    public Integer getRentalDuration() {
        return rentalDuration;
    }

    public void setRentalDuration(Integer rentalDuration) {
        this.rentalDuration = rentalDuration;
    }

    public Double getRentalRate() {
        return rentalRate;
    }

    public void setRentalRate(Double rentalRate) {
        this.rentalRate = rentalRate;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Double getReplacementCost() {
        return replacementCost;
    }

    public void setReplacementCost(Double replacementCost) {
        this.replacementCost = replacementCost;
    }

    public FilmRating getRating() {
        return rating;
    }

    public void setRating(FilmRating rating) {
        this.rating = rating;
    }

    public String[] getSpecialFeatures() {
        return specialFeatures;
    }

    public void setSpecialFeatures(String[] specialFeatures) {
        this.specialFeatures = specialFeatures;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }
}
