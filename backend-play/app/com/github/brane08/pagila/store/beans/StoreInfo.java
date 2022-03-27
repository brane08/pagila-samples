package com.github.brane08.pagila.store.beans;


import java.util.List;

public class StoreInfo {

	public Long storeId;
	public StaffInfo manager;
	public java.time.Instant lastUpdate;
	public AddressInfo address;
	public List<StaffInfo> currentStaff;
}
