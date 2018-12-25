package org.ops4j.graqulus.cdi.api;

public interface Serializer<S, T> {
    default T serialize(S object) {
        if (object == null) {
            return null;
        }
        return serializeNonNull(object);
    }

    T serializeNonNull(S object);

    default S deserialize(T serial) {
        if (serial == null) {
            return null;
        }
        return deserialize(serial);
    }

    S deserializeNonNull(T serial);
}
