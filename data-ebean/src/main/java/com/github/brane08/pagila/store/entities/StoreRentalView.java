package com.github.brane08.pagila.store.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "store_rental_view")
public class StoreRentalView {

    @Column(name = "store_id")
    private int storeId;

    @Id
    @Column(name = "rental_id")
    private int rentalId;

    @Column(name = "title")
    private String title;

    @Column(name = "customer")
    private String customer;

    @Column(name = "rental_date")
    private String rentalDate;

    @Column(name = "return_date")
    private String returnDate;

    @Column(name = "outstanding")
    private boolean outstanding;

    public int getStoreId() { return storeId; }
    public void setStoreId(int storeId) { this.storeId = storeId; }

    public int getRentalId() { return rentalId; }
    public void setRentalId(int rentalId) { this.rentalId = rentalId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }

    public String getRentalDate() { return rentalDate; }
    public void setRentalDate(String rentalDate) { this.rentalDate = rentalDate; }

    public String getReturnDate() { return returnDate; }
    public void setReturnDate(String returnDate) { this.returnDate = returnDate; }

    public boolean isOutstanding() { return outstanding; }
    public void setOutstanding(boolean outstanding) { this.outstanding = outstanding; }
}
