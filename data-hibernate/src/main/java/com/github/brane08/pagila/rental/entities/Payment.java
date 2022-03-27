package com.github.brane08.pagila.rental.entities;

import com.github.brane08.pagila.seedworks.entities.BaseModel;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "payment")
public class Payment extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    Integer paymentId;

    @Column(name = "customer_id")
    Integer customerId;

    @Column(name = "staff_id")
    Integer staffId;

    @Column(name = "rental_id")
    Integer rentalId;

    @Column(name = "amount")
    Float amount;

    @Column(name = "payment_date")
    Instant paymentDate;

    public Integer getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    public Integer getRentalId() {
        return rentalId;
    }

    public void setRentalId(Integer rentalId) {
        this.rentalId = rentalId;
    }

    public Float getAmount() {
        return amount;
    }

    public void setAmount(Float amount) {
        this.amount = amount;
    }

    public Instant getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Instant paymentDate) {
        this.paymentDate = paymentDate;
    }
}
