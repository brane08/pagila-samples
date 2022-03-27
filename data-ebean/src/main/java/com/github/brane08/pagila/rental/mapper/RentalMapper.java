package com.github.brane08.pagila.rental.mapper;

import com.github.brane08.pagila.rental.beans.RentalInfo;
import com.github.brane08.pagila.rental.entities.Rental;
import com.github.brane08.pagila.seedworks.mapper.CommonConfig;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = CommonConfig.class)
public interface RentalMapper {

    RentalInfo rentalToInfo(Rental rental);

    List<RentalInfo> rentalsToInfos(List<Rental> rentals);
}
