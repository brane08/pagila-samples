package com.github.brane08.pagila.seedworks.beans;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacetTest {

    @Test
    void singleArgConstructorStartsWithNoValues() {
        Facet facet = new Facet("rating");

        assertEquals("rating", facet.getProperty());
        assertTrue(facet.getValues().isEmpty());
    }

    @Test
    void addFacetValueAppendsKeyAndCount() {
        Facet facet = new Facet("rating");

        facet.addFacetValue("G", 4);
        facet.addFacetValue("PG", 2);

        assertEquals(2, facet.getValues().size());
        assertEquals("G", facet.getValues().getFirst().getKey());
        assertEquals(4, facet.getValues().getFirst().getValue());
        assertEquals("PG", facet.getValues().get(1).getKey());
        assertEquals(2, facet.getValues().get(1).getValue());
    }

    @Test
    void addAllValuesAppendsExistingFacetValues() {
        Facet facet = new Facet("category", new java.util.ArrayList<>(List.of(new Facet.FacetValue("Action", 1))));

        facet.addAllValues(List.of(new Facet.FacetValue("Drama", 5)));

        assertEquals(2, facet.getValues().size());
        assertEquals("Drama", facet.getValues().get(1).getKey());
        assertEquals(5, facet.getValues().get(1).getValue());
    }

    @Test
    void twoArgConstructorKeepsProvidedValues() {
        List<Facet.FacetValue> values = List.of(new Facet.FacetValue("x", 1));

        assertEquals(values, new Facet("p", values).getValues());
    }
}
