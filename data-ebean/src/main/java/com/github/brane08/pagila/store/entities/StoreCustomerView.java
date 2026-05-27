package com.github.brane08.pagila.store.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "store_customer_view")
public class StoreCustomerView {

    @Column(name = "store_id")
    private int storeId;

    @Id
    @Column(name = "customer_id")
    private int customerId;

    @Column(name = "customer")
    private String customer;

    @Column(name = "email")
    private String email;

    @Column(name = "rental_count")
    private int rentalCount;

    @Column(name = "total_spent")
    private BigDecimal totalSpent;

    public int getStoreId() { return storeId; }
    public void setStoreId(int storeId) { this.storeId = storeId; }

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
