package com.hotelaria.dtos.response;

public record AddressResponse(
        String street,
        String number,
        String complement,
        String district,
        String city,
        String state,
        String zipCode
) {
}
