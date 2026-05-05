package com.github.brane08.pagila.store.beans;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

public class StoreCustomerInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int customerId;
    private String customer;
    private String email;
    private int rentalCount;
    private BigDecimal totalSpent;

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getRentalCount() { return rentalCount; }
    public void setRentalCount(int rentalCount) { this.rentalCount = rentalCount; }

    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }
}
