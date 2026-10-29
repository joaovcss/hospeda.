package com.hotelaria.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Address {

    @Column(name = "address_street", length = 150)
    private String street;

    @Column(name = "address_number", length = 20)
    private String number;

    @Column(name = "address_complement", length = 100)
    private String complement;

    @Column(name = "address_district", length = 100)
    private String district;

    @Column(name = "address_city", length = 100)
    private String city;

    @Column(name = "address_state", length = 2)
    private String state;

    @Column(name = "address_zip_code", length = 8)
    private String zipCode;

    public Address() {
    }

    public Address(String street, String number, String complement, String district,
                   String city, String state, String zipCode) {
        this.street = street;
        this.number = number;
        this.complement = complement;
        this.district = district;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
    }

    public String getStreet() {
        return street;
    }

    public String getNumber() {
        return number;
    }

    public String getComplement() {
        return complement;
    }

    public String getDistrict() {
        return district;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getZipCode() {
        return zipCode;
    }
}
