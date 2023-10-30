package com.github.brane08.pagila.seedworks.app;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapBasedSortState implements ISortState<String> {

    private final Map<String, SortOrder> sortedMap;

    public MapBasedSortState() {
        this.sortedMap = new HashMap<>();
    }

    @Override
    public void setPropertySortOrder(String property, SortOrder order) {
        sortedMap.put(property, order);
    }

    @Override
    public SortOrder getPropertySortOrder(String property) {
        return sortedMap.getOrDefault(property, SortOrder.NONE);
    }

    public Set<String> propertySet() {
        return sortedMap.keySet();
    }

    @Override
    public String toString() {
        return "MapBasedSortState{" +
                "sortedMap=" + sortedMap +
                '}';
    }
}
