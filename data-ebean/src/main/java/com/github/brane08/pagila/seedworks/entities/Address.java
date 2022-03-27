package com.github.brane08.pagila.seedworks.entities;

import com.github.brane08.pagila.store.entities.Store;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "address")
public class Address extends BaseModel {
    @OneToOne(mappedBy = "address")
    public Store store;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    Integer addressId;
    @Column(name = "address")
    String address;
    @Column(name = "address2")
    String address2;
    @Column(name = "district")
    String district;
    @Column(name = "postal_code")
    String postalCode;
    @Column(name = "phone")
    String phone;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", columnDefinition = "int2")
    City city;

    public Integer getAddressId() {
        return addressId;
    }

    public void setAddressId(Integer addressId) {
        this.addressId = addressId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address that = (Address) o;
        return addressId.equals(that.addressId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addressId);
    }
}
