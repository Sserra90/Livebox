package com.creations.livebox;

import java.util.List;
import java.util.Objects;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Dummy model for testing serialization/deserialization
 */
public class Bag<T> {

    private String id;
    private List<T> values;

    @SuppressWarnings("WeakerAccess")
    public Bag() {
    }

    public Bag(String id, List<T> values) {
        this.id = id;
        this.values = values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bag<?> bag = (Bag<?>) o;
        return id.equals(bag.id) &&
                Objects.equals(values, bag.values);
    }

    @Override
    public String toString() {
        return "Bag{" +
                "id='" + id + '\'' +
                ", values=" + values +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, values);
    }
}
