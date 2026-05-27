package com.github.brane08.pagila.store.beans;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

public class SalesByStoreInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String store;
    private String manager;
    private BigDecimal totalSales;

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }
}
