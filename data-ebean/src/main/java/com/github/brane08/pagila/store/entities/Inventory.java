package com.github.brane08.pagila.store.entities;

import com.github.brane08.pagila.film.entities.Film;
import com.github.brane08.pagila.seedworks.entities.BaseModel;
import jakarta.persistence.*;

@Entity
@Table(name = "inventory")
public class Inventory extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    Integer inventoryId;

    @Column(name = "film_id", insertable = false, updatable = false)
    Integer filmId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "film_id")
    Film film;

    @Column(name = "store_id", insertable = false, updatable = false)
    Integer storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    Store store;

    public Integer getInventoryId() { return inventoryId; }
    public void setInventoryId(Integer inventoryId) { this.inventoryId = inventoryId; }

    public Integer getFilmId() { return filmId; }
    public void setFilmId(Integer filmId) { this.filmId = filmId; }

    public Film getFilm() { return film; }
    public void setFilm(Film film) { this.film = film; }

    public Integer getStoreId() { return storeId; }
    public void setStoreId(Integer storeId) { this.storeId = storeId; }

    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
}
