package org.ops4j.graqulus.shared;

import java.lang.reflect.Field;

import io.earcam.unexceptional.Exceptional;

public class ReflectionHelper {

    private ReflectionHelper() {
        // do not instantiate
    }

    @SuppressWarnings("unchecked")
    public static <T> T readField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException exc) {
            throw Exceptional.throwAsUnchecked(exc);
        }
    }

    public static void writeField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException exc) {
            throw Exceptional.throwAsUnchecked(exc);
        }
    }
}
