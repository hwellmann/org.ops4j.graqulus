package org.ops4j.graqulus.cdi.api;

import java.util.Collections;
import java.util.List;

public interface Resolver<T> {

    default List<String> loadById() {
        return Collections.emptyList();
    }

    default boolean loadAllById() {
        return false;
    }
}
