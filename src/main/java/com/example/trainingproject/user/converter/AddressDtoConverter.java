package com.example.trainingproject.user.converter;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.example.trainingproject.openapi.dto.AddressDto;
import com.example.trainingproject.user.entity.Address;

@SuppressWarnings("NullableProblems")
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AddressDtoConverter {

    @Named("toAddressDto")
    AddressDto toDto(final Address entity);
}
