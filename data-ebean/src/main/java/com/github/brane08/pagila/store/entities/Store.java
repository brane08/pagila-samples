package com.github.brane08.pagila.store.entities;

import com.github.brane08.pagila.seedworks.entities.Address;
import com.github.brane08.pagila.seedworks.entities.BaseModel;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "store")
public class Store extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    Integer storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_staff_id")
    Staff manager;

    @OneToOne(optional = false)
    @JoinColumn(name = "address_id")
    Address address;

    public Integer getStoreId() {
        return storeId;
    }

    public void setStoreId(Integer storeId) {
        this.storeId = storeId;
    }

    public Staff getManager() {
        return manager;
    }

    public void setManager(Staff manager) {
        this.manager = manager;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Store that = (Store) o;
        return storeId.equals(that.storeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storeId);
    }
}
