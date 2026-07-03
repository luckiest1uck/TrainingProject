package com.example.trainingproject.user.stub;

import java.util.UUID;

import com.example.trainingproject.openapi.dto.AddressDto;
import com.example.trainingproject.openapi.dto.UserDto;
import com.example.trainingproject.user.entity.Address;
import com.example.trainingproject.user.entity.UserEntity;

@SuppressWarnings("unused")
public class UserDtoTestStub {

    public static UserEntity createUserEntity() {
        Address address = AddressDtoTestStub.createAddressEntity();

        UserEntity entity = new UserEntity();
        entity.setId(UUID.fromString("ebd4d43f-3152-4af5-86dd-526a002cbbc3"));
        entity.setFirstName("John");
        entity.setLastName("Doe");
        entity.setEmail("johndoe@example.com");
        entity.setPassword("password123");
        entity.setStripeCustomerToken("tok_123456789");
        entity.setAddress(address);
        return entity;
    }

    public static UserDto createUserDto() {
        AddressDto addressDto = AddressDtoTestStub.createAddressDto();

        UserDto dto = new UserDto();
        dto.setId(UUID.fromString("ebd4d43f-3152-4af5-86dd-526a002cbbc3"));
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setEmail("johndoe@example.com");
        dto.setStripeCustomerToken("tok_123456789");
        dto.setAddress(addressDto);
        return dto;
    }
}
