package com.github.brane08.pagila.film.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "film_category_facet")
public class FilmCategoryFacet {

    @Id
    @Column(name = "key")
    private String key;

    @Column(name = "value")
    private int value;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
}
