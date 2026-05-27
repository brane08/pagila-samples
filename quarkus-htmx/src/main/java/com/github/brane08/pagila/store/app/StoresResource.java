package com.github.brane08.pagila.store.app;

import com.github.brane08.pagila.store.beans.SalesByStoreInfo;
import com.github.brane08.pagila.store.beans.StaffViewInfo;
import com.github.brane08.pagila.store.beans.StoreCustomerInfo;
import com.github.brane08.pagila.store.beans.StoreInventoryInfo;
import com.github.brane08.pagila.store.beans.StoreRentalInfo;
import com.github.brane08.pagila.store.beans.StoreViewInfo;
import com.github.brane08.pagila.store.mapper.StoreMapper;
import com.github.brane08.pagila.store.repositories.StoreRepository;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import java.util.List;

@Path("/stores")
public class StoresResource {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance salesByStore(List<SalesByStoreInfo> sales);
        public static native TemplateInstance staffList(List<StaffViewInfo> staff);
        public static native TemplateInstance storeList(List<StoreViewInfo> stores);
        public static native TemplateInstance storeDetail(StoreViewInfo store, List<StoreInventoryInfo> items);
        public static native TemplateInstance inventory(StoreViewInfo store, List<StoreInventoryInfo> items);
        public static native TemplateInstance rentals(StoreViewInfo store, List<StoreRentalInfo> rentals);
        public static native TemplateInstance customers(StoreViewInfo store, List<StoreCustomerInfo> customers);
    }

    private final StoreRepository repository;
    private final StoreMapper mapper;

    @Inject
    public StoresResource(StoreRepository repository, StoreMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @GET
    public TemplateInstance storeList() {
        return Templates.storeList(repository.listStoreViews());
    }

    @GET
    @Path("/@sales-by-store")
    public TemplateInstance salesByStore() {
        return Templates.salesByStore(repository.allSalesByStore());
    }

    @GET
    @Path("/@staff")
    public TemplateInstance staffList() {
        return Templates.staffList(repository.listStaffViews());
    }

    @GET
    @Path("/{storeId}")
    public TemplateInstance storeDetail(@PathParam("storeId") int storeId) {
        StoreViewInfo store = repository.findStoreView(storeId)
                .orElseThrow(NotFoundException::new);
        return Templates.storeDetail(store, repository.getStoreInventory(storeId));
    }

    @GET
    @Path("/{storeId}/inventory")
    public TemplateInstance storeInventory(@PathParam("storeId") int storeId) {
        StoreViewInfo store = repository.findStoreView(storeId)
                .orElseThrow(NotFoundException::new);
        return Templates.inventory(store, repository.getStoreInventory(storeId));
    }

    @GET
    @Path("/{storeId}/rentals")
    public TemplateInstance storeRentals(@PathParam("storeId") int storeId) {
        StoreViewInfo store = repository.findStoreView(storeId)
                .orElseThrow(NotFoundException::new);
        return Templates.rentals(store, repository.getStoreRentals(storeId));
    }

    @GET
    @Path("/{storeId}/customers")
    public TemplateInstance storeCustomers(@PathParam("storeId") int storeId) {
        StoreViewInfo store = repository.findStoreView(storeId)
                .orElseThrow(NotFoundException::new);
        return Templates.customers(store, repository.getStoreTopCustomers(storeId));
    }
}
