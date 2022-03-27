package com.github.brane08.pagila.rental.mapper;

import com.github.brane08.pagila.rental.beans.CustomerViewInfo;
import com.github.brane08.pagila.rental.entities.CustomerView;
import com.github.brane08.pagila.seedworks.mapper.CommonConfig;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = CommonConfig.class)
public interface RentalMapper {

    CustomerViewInfo customerViewToInfo(CustomerView view);

    List<CustomerViewInfo> customerViewsToInfo(List<CustomerView> view);
}
