package com.github.brane08.pagila.seedworks.beans;

import java.util.ArrayList;
import java.util.List;

public class Facet {

    private final String property;
    private final List<FacetValue> values;

    public Facet(String property) {
        this(property, new ArrayList<>());
    }

    public Facet(String property, List<FacetValue> values) {
        this.property = property;
        this.values = values;
    }

    public String getProperty() {
        return property;
    }

    public List<FacetValue> getValues() {
        return values;
    }

    public void addFacetValue(String key, int value) {
        this.values.add(new FacetValue(key, value));
    }

    public void addAllValues(List<FacetValue> values) {
        this.values.addAll(values);
    }

    public static class FacetValue {
        private final String key;
        private final int value;

        public FacetValue(String key, int value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public int getValue() {
            return value;
        }
    }
}
