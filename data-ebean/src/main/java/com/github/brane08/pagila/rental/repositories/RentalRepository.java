package com.github.brane08.pagila.rental.repositories;

import com.github.brane08.pagila.rental.entities.Rental;
import com.github.brane08.pagila.rental.mapper.RentalMapper;
import com.github.brane08.pagila.seedworks.repositories.EbeanRepository;
import io.ebean.Database;
import org.mapstruct.factory.Mappers;

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
}
