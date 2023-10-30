package com.github.brane08.pagila.rental.beans;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

public class RentalInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    Integer rentalId;
    Instant rentalDate;
    Integer inventoryId;
    Integer customerId;
    Instant returnDate;
    Integer staffId;
    Instant lastUpdate;

    public Integer getRentalId() {
        return rentalId;
    }

    public void setRentalId(Integer rentalId) {
        this.rentalId = rentalId;
    }

    public Instant getRentalDate() {
        return rentalDate;
    }

    public void setRentalDate(Instant rentalDate) {
        this.rentalDate = rentalDate;
    }

    public Integer getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Integer inventoryId) {
        this.inventoryId = inventoryId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Instant getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(Instant returnDate) {
        this.returnDate = returnDate;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    public Instant getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Instant lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
