package com.github.brane08.pagila.store.beans;


import com.github.brane08.pagila.seedworks.beans.AddressInfo;

import java.time.Instant;
import java.util.List;

public class StoreInfo {

    Long storeId;
    StaffInfo manager;
    java.time.Instant lastUpdate;
    AddressInfo address;
    List<StaffInfo> currentStaff;

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public StaffInfo getManager() {
        return manager;
    }

    public void setManager(StaffInfo manager) {
        this.manager = manager;
    }

    public Instant getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Instant lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public AddressInfo getAddress() {
        return address;
    }

    public void setAddress(AddressInfo address) {
        this.address = address;
    }

    public List<StaffInfo> getCurrentStaff() {
        return currentStaff;
    }

    public void setCurrentStaff(List<StaffInfo> currentStaff) {
        this.currentStaff = currentStaff;
    }
}
