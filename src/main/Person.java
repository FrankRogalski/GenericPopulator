package main;

import java.util.List;

public class Person {
    private String name;
    private int age;
    private String country;
    private String zipCode;
    private String street;
    private int houseNumber;
    private Person partner;
    private List<Person> children;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(final int age) {
        this.age = age;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(final String zipCode) {
        this.zipCode = zipCode;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(final String street) {
        this.street = street;
    }

    public int getHouseNumber() {
        return houseNumber;
    }

    public void setHouseNumber(final int houseNumber) {
        this.houseNumber = houseNumber;
    }

    public Person getPartner() {
        return partner;
    }

    public void setPartner(final Person partner) {
        this.partner = partner;
    }

    public List<Person> getChildren() {
        return children;
    }

    public void setChildren(final List<Person> children) {
        this.children = children;
    }
}
