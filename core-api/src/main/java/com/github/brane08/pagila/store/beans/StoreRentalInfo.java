package com.github.brane08.pagila.store.beans;

import java.io.Serial;
import java.io.Serializable;

public class StoreRentalInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int rentalId;
    private String title;
    private String customer;
    private String rentalDate;
    private String returnDate;
    private boolean outstanding;

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
