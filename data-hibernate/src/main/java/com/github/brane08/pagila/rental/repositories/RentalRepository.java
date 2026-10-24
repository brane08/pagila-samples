package com.github.brane08.pagila.rental.repositories;

import com.github.brane08.pagila.rental.beans.CustomerViewInfo;
import com.github.brane08.pagila.rental.entities.CustomerView;
import com.github.brane08.pagila.rental.entities.Rental;
import com.github.brane08.pagila.rental.mapper.RentalMapper;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import com.github.brane08.pagila.seedworks.query.QueryParser;
import com.github.brane08.pagila.seedworks.repositories.JPARepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class RentalRepository extends JPARepository<Rental, Integer> {

    private final RentalMapper mapper = RentalMapper.INSTANCE;

    public RentalRepository(EntityManager em) {
        super(em);
    }

    @Override
    protected Class<Rental> entityClass() {
        return Rental.class;
    }

    @Override
    protected String filterProperty() {
        return "rentalId";
    }

    public PagedList<CustomerViewInfo> listCustomerViews(int offset, int size, String order) {
        QueryParser<CustomerView> parser = buildParser("", CustomerView.class);
        List<CustomerView> list = parser.getResults(offset, size, order);
        long count = parser.getCount();
        return new PagedList<>(mapper.customerViewsToInfo(list), (int) count);
    }
}
