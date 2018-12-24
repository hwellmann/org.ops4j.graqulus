package org.ops4j.graqulus.shared;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

    @SuppressWarnings("unchecked")
    public static <T> T invokeMethod(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return (T) method.invoke(target);
        } catch (InvocationTargetException exc) {
            throw Exceptional.throwAsUnchecked(exc.getCause());
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException  exc) {
            throw Exceptional.throwAsUnchecked(exc);
        }
    }
}
