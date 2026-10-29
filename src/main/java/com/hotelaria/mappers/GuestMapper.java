package com.hotelaria.mappers;

import com.hotelaria.dtos.request.AddressRequest;
import com.hotelaria.dtos.response.AddressResponse;
import com.hotelaria.dtos.response.GuestResponse;
import com.hotelaria.dtos.response.GuestSummaryResponse;
import com.hotelaria.models.Address;
import com.hotelaria.models.Guest;
import com.hotelaria.utils.CpfUtils;
import org.springframework.stereotype.Component;

@Component
public class GuestMapper {

    public GuestResponse toResponse(Guest guest) {
        return new GuestResponse(
                guest.getId(),
                guest.getUser().getId(),
                guest.getUser().getFullName(),
                guest.getUser().getEmail(),
                guest.getCpf(),
                guest.getPhone(),
                guest.getBirthDate(),
                toAddressResponse(guest.getAddress()),
                guest.getUser().isActive(),
                guest.getCreatedAt());
    }

    public GuestSummaryResponse toSummary(Guest guest) {
        return new GuestSummaryResponse(
                guest.getId(),
                guest.getUser().getFullName(),
                guest.getUser().getEmail(),
                CpfUtils.mask(guest.getCpf()));
    }

    public Address toAddress(AddressRequest request) {
        if (request == null) {
            return null;
        }
        return new Address(
                request.street(),
                request.number(),
                request.complement(),
                request.district(),
                request.city(),
                request.state() == null ? null : request.state().toUpperCase(),
                request.zipCode() == null ? null : request.zipCode().replace("-", ""));
    }

    private AddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponse(
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getDistrict(),
                address.getCity(),
                address.getState(),
                address.getZipCode());
    }
}
