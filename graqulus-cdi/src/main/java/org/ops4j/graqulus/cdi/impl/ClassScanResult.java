package org.ops4j.graqulus.cdi.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.DefinitionException;

@Vetoed
public class ClassScanResult {

    private String schemaPath;
    private String modelPackage;
    private Map<String, AnnotatedMethod<?>> queryMethodMap = new HashMap<>();
    private Map<String, AnnotatedMethod<?>> batchLoaderMap = new HashMap<>();
    private Map<String, Bean<?>> typeResolverMap = new HashMap<>();

    public String getSchemaPath() {
        return schemaPath;
    }

    public void setSchemaPath(String schemaPath) {
        this.schemaPath = schemaPath;
    }

    public String getModelPackage() {
        return modelPackage;
    }

    public void setModelPackage(String modelPackage) {
        this.modelPackage = modelPackage;
    }

    public void registerQueryMethod(AnnotatedMethod<?> method) {
        AnnotatedMethod<?> previous = queryMethodMap.putIfAbsent(method.getJavaMember().getName(), method);
        if (previous != null) {
            throw new DefinitionException("duplicate query method");
        }
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
            throw new DefinitionException("duplicate type resolver class");
        }
    }

    public AnnotatedMethod<?> getBatchLoaderMethod(String typeName) {
        return batchLoaderMap.get(typeName);
    }

    public Map<String, AnnotatedMethod<?>> getBatchLoaderMethods() {
        return Collections.unmodifiableMap(batchLoaderMap);
    }

    public AnnotatedMethod<?> getQueryMethod(String queryName) {
        return queryMethodMap.get(queryName);
    }

    public Bean<?> getTypeResolver(String typeName) {
        return typeResolverMap.get(typeName);
    }


}
