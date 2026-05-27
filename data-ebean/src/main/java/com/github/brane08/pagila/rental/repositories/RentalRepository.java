package com.github.brane08.pagila.rental.repositories;

import com.github.brane08.pagila.rental.beans.CustomerViewInfo;
import com.github.brane08.pagila.rental.entities.CustomerView;
import com.github.brane08.pagila.rental.entities.Rental;
import com.github.brane08.pagila.rental.mapper.RentalMapper;
import com.github.brane08.pagila.seedworks.beans.PagedList;
import com.github.brane08.pagila.seedworks.query.QueryParser;
import com.github.brane08.pagila.seedworks.repositories.EbeanRepository;
import io.ebean.Database;
import org.mapstruct.factory.Mappers;

import java.util.List;

public class RentalRepository extends EbeanRepository<Rental, Integer> {

    private final RentalMapper mapper;

    public RentalRepository(Database db) {
        super(db);
        this.mapper = Mappers.getMapper(RentalMapper.class);
    }

    @Override
    protected Class<Rental> entityClass() {
        return Rental.class;
    }

    @Override
    protected String filterProperty() {
        return null;
    }

    public PagedList<CustomerViewInfo> listCustomerViews(int offset, int size, String order) {
        QueryParser<CustomerView> parser = buildParser("", CustomerView.class);
        List<CustomerView> list = parser.getResults(offset, size, order);
        long count = parser.getCount();
        return new PagedList<>(mapper.customerViewsToInfos(list), (int) count);
    }
}
