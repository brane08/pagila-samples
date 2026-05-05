package com.github.brane08.pagila.store.repositories;

import com.github.brane08.pagila.seedworks.repositories.EbeanRepository;
import com.github.brane08.pagila.store.beans.SalesByStoreInfo;
import com.github.brane08.pagila.store.beans.StaffViewInfo;
import com.github.brane08.pagila.store.beans.StoreCustomerInfo;
import com.github.brane08.pagila.store.beans.StoreInventoryInfo;
import com.github.brane08.pagila.store.beans.StoreRentalInfo;
import com.github.brane08.pagila.store.beans.StoreViewInfo;
import com.github.brane08.pagila.store.entities.SalesByStore;
import com.github.brane08.pagila.store.entities.StaffView;
import com.github.brane08.pagila.store.entities.Store;
import com.github.brane08.pagila.store.mapper.StoreMapper;
import io.ebean.Database;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Optional;

public class StoreRepository extends EbeanRepository<Store, Integer> {

    private final StoreMapper mapper;

    public StoreRepository(Database db) {
        super(db);
        this.mapper = Mappers.getMapper(StoreMapper.class);
    }

    @Override
    protected List<String> getLoadFields() {
        return List.of("currentStaff");
    }

    @Override
    protected Class<Store> entityClass() {
        return Store.class;
    }

    @Override
    protected String filterProperty() {
        return "storeId";
    }

    public List<SalesByStoreInfo> allSalesByStore() {
        List<SalesByStore> list = db().find(SalesByStore.class).findList();
        return mapper.salesByStoresToInfos(list);
    }

    public List<StaffViewInfo> listStaffViews() {
        List<StaffView> list = db().find(StaffView.class).findList();
        return mapper.staffViewsToInfo(list);
    }

    private static final String STORE_FROM_SQL =
        "SELECT s.store_id, st.first_name || ' ' || st.last_name AS manager, " +
        "a.address, a.district, c.city " +
        "FROM store s " +
        "JOIN staff st ON s.manager_staff_id = st.staff_id " +
        "JOIN address a ON s.address_id = a.address_id " +
        "JOIN city c ON a.city_id = c.city_id";

    public List<StoreViewInfo> listStoreViews() {
        return db().findDto(StoreViewInfo.class, STORE_FROM_SQL + " ORDER BY s.store_id").findList();
    }

    public Optional<StoreViewInfo> findStoreView(int storeId) {
        return Optional.ofNullable(
            db().findDto(StoreViewInfo.class, STORE_FROM_SQL + " WHERE s.store_id = :storeId")
                .setParameter("storeId", storeId)
                .findOne()
        );
    }

    private static final String INVENTORY_SQL =
        "SELECT f.film_id, f.title, c.name AS category, f.rating::text AS rating, f.rental_rate, " +
        "COUNT(i.inventory_id) AS total_copies, " +
        "SUM(CASE WHEN r.rental_id IS NULL OR r.return_date IS NOT NULL THEN 1 ELSE 0 END) AS available_copies " +
        "FROM film f " +
        "JOIN film_category fc ON f.film_id = fc.film_id " +
        "JOIN category c ON fc.category_id = c.category_id " +
        "JOIN inventory i ON f.film_id = i.film_id AND i.store_id = :storeId " +
        "LEFT JOIN rental r ON i.inventory_id = r.inventory_id AND r.return_date IS NULL " +
        "GROUP BY f.film_id, f.title, c.name, f.rating, f.rental_rate " +
        "ORDER BY available_copies DESC, f.title " +
        "LIMIT 50";

    public List<StoreInventoryInfo> getStoreInventory(int storeId) {
        return db().findDto(StoreInventoryInfo.class, INVENTORY_SQL)
                .setParameter("storeId", storeId)
                .findList();
    }

    private static final String RENTALS_SQL =
        "SELECT r.rental_id, f.title, cu.first_name || ' ' || cu.last_name AS customer, " +
        "TO_CHAR(r.rental_date, 'YYYY-MM-DD') AS rental_date, " +
        "COALESCE(TO_CHAR(r.return_date, 'YYYY-MM-DD'), '—') AS return_date, " +
        "(r.return_date IS NULL) AS outstanding " +
        "FROM rental r " +
        "JOIN inventory i ON r.inventory_id = i.inventory_id " +
        "JOIN film f ON i.film_id = f.film_id " +
        "JOIN customer cu ON r.customer_id = cu.customer_id " +
        "WHERE i.store_id = :storeId " +
        "ORDER BY r.rental_date DESC " +
        "LIMIT 50";

    public List<StoreRentalInfo> getStoreRentals(int storeId) {
        return db().findDto(StoreRentalInfo.class, RENTALS_SQL)
                .setParameter("storeId", storeId)
                .findList();
    }

    private static final String CUSTOMERS_SQL =
        "SELECT cu.customer_id, cu.first_name || ' ' || cu.last_name AS customer, cu.email, " +
        "COUNT(r.rental_id) AS rental_count, COALESCE(SUM(p.amount), 0) AS total_spent " +
        "FROM customer cu " +
        "JOIN rental r ON cu.customer_id = r.customer_id " +
        "JOIN inventory i ON r.inventory_id = i.inventory_id " +
        "LEFT JOIN payment p ON r.rental_id = p.rental_id " +
        "WHERE i.store_id = :storeId " +
        "GROUP BY cu.customer_id, cu.first_name, cu.last_name, cu.email " +
        "ORDER BY rental_count DESC, total_spent DESC " +
        "LIMIT 20";

    public List<StoreCustomerInfo> getStoreTopCustomers(int storeId) {
        return db().findDto(StoreCustomerInfo.class, CUSTOMERS_SQL)
                .setParameter("storeId", storeId)
                .findList();
    }
}
