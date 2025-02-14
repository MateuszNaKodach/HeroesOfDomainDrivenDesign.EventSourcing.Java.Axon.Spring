package com.dddheroes.heroesofddd.creaturerecruitment.write;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class DwellingIdTest {

    @Test
    void shouldThrowExceptionWhenRawIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new DwellingId(null));
        assertEquals("Dwelling id cannot be null or empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRawIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new DwellingId(""));
        assertEquals("Dwelling id cannot be null or empty", exception.getMessage());
    }

    @Test
    void shouldAddAggregateTypeWhenNotPresent() {
        DwellingId dwellingId = new DwellingId("12345");
        assertEquals("Dwelling:12345", dwellingId.raw());
    }

    @Test
    void shouldNotAddAggregateTypeWhenAlreadyPresent() {
        DwellingId dwellingId = new DwellingId("Dwelling:12345");
        assertEquals("Dwelling:12345", dwellingId.raw());
    }

    @Test
    void factoryMethodShouldAddAggregateTypeWhenNotPresent() {
        DwellingId dwellingId = DwellingId.of("12345");
        assertEquals("Dwelling:12345", dwellingId.raw());
    }

    @Test
    void factoryMethodShouldNotAddAggregateTypeWhenAlreadyPresent() {
        DwellingId dwellingId = new DwellingId("Dwelling:12345");
        assertEquals("Dwelling:12345", dwellingId.raw());
    }

    @Test
    void shouldReturnRawStringInToString() {
        DwellingId dwellingId = new DwellingId("12345");
        assertEquals("Dwelling:12345", dwellingId.toString());
    }
}