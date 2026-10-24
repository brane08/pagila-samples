package com.github.brane08.pagila.film.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "film_price_facet")
public class FilmPriceFacet {

    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "price_0_1")
    private int price01;

    @Column(name = "price_1_3")
    private int price13;

    @Column(name = "price_3_9")
    private int price39;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPrice01() { return price01; }
    public void setPrice01(int price01) { this.price01 = price01; }

    public int getPrice13() { return price13; }
    public void setPrice13(int price13) { this.price13 = price13; }

    public int getPrice39() { return price39; }
    public void setPrice39(int price39) { this.price39 = price39; }
}
