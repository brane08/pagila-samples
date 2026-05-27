package com.github.brane08.pagila.rental.entities;

import com.github.brane08.pagila.seedworks.entities.Address;
import com.github.brane08.pagila.seedworks.entities.BaseModel;
import com.github.brane08.pagila.store.entities.Store;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "customer")
public class Customer extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    Integer customerId;

    @Column(name = "store_id", insertable = false, updatable = false)
    Integer storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", columnDefinition = "int2")
    Store store;

    @Column(name = "first_name")
    String firstName;

    @Column(name = "last_name")
    String lastName;

    @Column(name = "email")
    String email;

    @Column(name = "address_id", insertable = false, updatable = false)
    Integer addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", columnDefinition = "int2")
    Address address;

    @Column(name = "activebool")
    boolean activeBool;

    @Column(name = "create_date")
    LocalDate createDate;

    @Column(name = "active")
    Short active;

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public Integer getStoreId() { return storeId; }
    public void setStoreId(Integer storeId) { this.storeId = storeId; }

    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getAddressId() { return addressId; }
    public void setAddressId(Integer addressId) { this.addressId = addressId; }

    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }

    public boolean isActiveBool() { return activeBool; }
    public void setActiveBool(boolean activeBool) { this.activeBool = activeBool; }

    public LocalDate getCreateDate() { return createDate; }
    public void setCreateDate(LocalDate createDate) { this.createDate = createDate; }

    public Short getActive() { return active; }
    public void setActive(Short active) { this.active = active; }
}
