package com.github.brane08.pagila.store.beans;


import java.time.Instant;

public class AddressInfo {

	public Long addressId;
	public String address;
	public String address2;
	public String district;
	public String postalCode;
	public String phone;
	public Instant lastUpdate;
	public CityInfo city;
}
