package com.github.brane08.pagila.film.beans;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

public class SalesByFilmCategoryInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String category;
    private BigDecimal totalSales;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }
}
