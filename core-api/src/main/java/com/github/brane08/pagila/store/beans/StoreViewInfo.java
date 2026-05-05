package com.github.brane08.pagila.store.beans;

import java.io.Serial;
import java.io.Serializable;

public class StoreViewInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int storeId;
    private String manager;
    private String address;
    private String district;
    private String city;

    public int getStoreId() { return storeId; }
    public void setStoreId(int storeId) { this.storeId = storeId; }

    public String getManager() { return manager; }
    public void setManager(String manager) { this.manager = manager; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
