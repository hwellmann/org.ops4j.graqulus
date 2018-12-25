package org.ops4j.graqulus.cdi.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.DefinitionException;

@Vetoed
public class ClassScanResult {

    private Map<String, AnnotatedMethod<?>> batchLoaderMap = new HashMap<>();
    private List<AnnotatedType<?>> rootOperations = new ArrayList<>();

    /** Maps GraphQL types to type resolver beans. */
    private Map<String, Bean<?>> typeResolverMap = new HashMap<>();

    /** Maps Java class names to serializers */
    private Map<String, Bean<?>> serializerMap = new HashMap<>();

    public void registerRootOperation(AnnotatedType<?> rootOperation) {
        rootOperations.add(rootOperation);
    }

    public void registerBatchLoaderMethod(AnnotatedMethod<?> method) {
        Type returnType = method.getJavaMember().getGenericReturnType();
        if (returnType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) returnType;
            if (!paramType.getRawType().getTypeName().equals(List.class.getName())) {
                throw new DefinitionException("batch loader method must return java.util.List<T>");
            }
            Type itemType = paramType.getActualTypeArguments()[0];
            Class<?> itemClass = (Class<?>) itemType;

            AnnotatedMethod<?> previous = batchLoaderMap.putIfAbsent(itemClass.getSimpleName(), method);
            if (previous != null) {
                throw new DefinitionException("duplicate batch loader method");
            }
        }
    }

    public void registerTypeResolver(String typeName, Bean<?> bean) {
        Bean<?> previous = typeResolverMap.putIfAbsent(typeName, bean);
        if (previous != null) {
            throw new DefinitionException("Duplicate type resolver class");
        }
    }

    public void registerSerializer(String javaClassName, Bean<?> bean) {
        Bean<?> previous = serializerMap.putIfAbsent(javaClassName, bean);
        if (previous != null) {
            throw new DefinitionException("Duplicate serializer for class " + javaClassName);
        }
    }

    public AnnotatedMethod<?> getBatchLoaderMethod(String typeName) {
        return batchLoaderMap.get(typeName);
    }

    public Map<String, AnnotatedMethod<?>> getBatchLoaderMethods() {
        return Collections.unmodifiableMap(batchLoaderMap);
    }

    public Bean<?> getTypeResolver(String typeName) {
        return typeResolverMap.get(typeName);
    }

    public Bean<?> getSerializer(String javaClassName) {
        return typeResolverMap.get(javaClassName);
    }

    public List<AnnotatedType<?>> getRootOperations() {
        return rootOperations;
    }
}
