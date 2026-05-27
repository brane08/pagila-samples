package com.github.brane08.pagila.store.beans;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

public class StoreInventoryInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int filmId;
    private String title;
    private String category;
    private String rating;
    private BigDecimal rentalRate;
    private int totalCopies;
    private int availableCopies;

    public int getFilmId() { return filmId; }
    public void setFilmId(int filmId) { this.filmId = filmId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }

    public BigDecimal getRentalRate() { return rentalRate; }
    public void setRentalRate(BigDecimal rentalRate) { this.rentalRate = rentalRate; }

    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }

    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }
}
