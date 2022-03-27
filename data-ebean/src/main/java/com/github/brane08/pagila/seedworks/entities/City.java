package com.github.brane08.pagila.seedworks.entities;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "city")
public class City extends BaseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "city_id")
    Integer cityId;

    @Column(name = "city")
    String city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", columnDefinition = "int2")
    Country country;

    public Integer getCityId() {
        return cityId;
    }

    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        City that = (City) o;
        return Objects.equals(city, that.city) && Objects.equals(country, that.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(city, country);
    }
}
