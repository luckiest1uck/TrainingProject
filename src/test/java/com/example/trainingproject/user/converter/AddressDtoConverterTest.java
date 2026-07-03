package com.example.trainingproject.user.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.example.trainingproject.openapi.dto.AddressDto;
import com.example.trainingproject.user.entity.Address;
import com.example.trainingproject.user.stub.AddressDtoTestStub;

class AddressDtoConverterTest {

    private final AddressDtoConverter converter = Mappers.getMapper(AddressDtoConverter.class);

    @Test
    @DisplayName("toDto should convert Address to AddressDto")
    void toDtoShouldConvertAddressToAddressDto() {
        Address address = AddressDtoTestStub.createAddressEntity();

        AddressDto dto = converter.toDto(address);

        assertEquals(address.getLine(), dto.getLine());
        assertEquals(address.getCity(), dto.getCity());
        assertEquals(address.getCountry(), dto.getCountry());
        assertEquals(address.getPostcode(), dto.getPostcode());
    }
}
