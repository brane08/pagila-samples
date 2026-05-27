package com.github.brane08.pagila.rental.entities;

import com.github.brane08.pagila.seedworks.entities.BaseModel;
import com.github.brane08.pagila.store.entities.Inventory;
import com.github.brane08.pagila.store.entities.Staff;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "rental")
public class Rental extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rental_id")
    Integer rentalId;

    @Column(name = "rental_date")
    Instant rentalDate;

    @Column(name = "inventory_id", insertable = false, updatable = false)
    Integer inventoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id")
    Inventory inventory;

    @Column(name = "customer_id", insertable = false, updatable = false)
    Integer customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    Customer customer;

    @Column(name = "return_date")
    Instant returnDate;

    @Column(name = "staff_id", insertable = false, updatable = false)
    Integer staffId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    Staff staff;

    public Integer getRentalId() { return rentalId; }
    public void setRentalId(Integer rentalId) { this.rentalId = rentalId; }

    public Instant getRentalDate() { return rentalDate; }
    public void setRentalDate(Instant rentalDate) { this.rentalDate = rentalDate; }

    public Integer getInventoryId() { return inventoryId; }
    public void setInventoryId(Integer inventoryId) { this.inventoryId = inventoryId; }

    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public Instant getReturnDate() { return returnDate; }
    public void setReturnDate(Instant returnDate) { this.returnDate = returnDate; }

    public Integer getStaffId() { return staffId; }
    public void setStaffId(Integer staffId) { this.staffId = staffId; }

    public Staff getStaff() { return staff; }
    public void setStaff(Staff staff) { this.staff = staff; }
}
