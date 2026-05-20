export interface SalesByStore {
  store: string;
  manager: string;
  totalSales: number;
}

export interface StoreStaffInfo {
  staffId: number;
  firstName: string;
  lastName: string;
  email: string;
  username: string;
}

export interface StoreAddressInfo {
  address: string;
  address2: string | null;
  district: string;
  postalCode: string;
  phone: string;
  city: { city: string; country: { country: string } };
}

export interface StoreDetail {
  storeId: number;
  manager: StoreStaffInfo;
  address: StoreAddressInfo;
  currentStaff: StoreStaffInfo[];
}
