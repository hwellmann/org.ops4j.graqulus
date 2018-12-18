package org.ops4j.graqulus.generator.java;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.ScalarTypeDefinition;
import graphql.language.Type;
import graphql.language.TypeName;
import graphql.schema.idl.TypeDefinitionRegistry;

public class JavaTypeMapper {

    private static final Map<String, String> BUILT_IN_SCALARS;
    private static final Map<String, String> NON_NULL_SCALARS;

    static {
        BUILT_IN_SCALARS = new HashMap<>();
        BUILT_IN_SCALARS.put("Boolean", Boolean.class.getSimpleName());
        BUILT_IN_SCALARS.put("Float", Float.class.getSimpleName());
        BUILT_IN_SCALARS.put("ID", String.class.getSimpleName());
        BUILT_IN_SCALARS.put("Int", Integer.class.getSimpleName());
        BUILT_IN_SCALARS.put("String", String.class.getSimpleName());

        NON_NULL_SCALARS = new HashMap<>();
        NON_NULL_SCALARS.put(Boolean.class.getSimpleName(), boolean.class.getSimpleName());
        NON_NULL_SCALARS.put(Float.class.getSimpleName(), float.class.getSimpleName());
        NON_NULL_SCALARS.put(Integer.class.getSimpleName(), int.class.getSimpleName());
    }

    private TypeDefinitionRegistry registry;

    public JavaTypeMapper(TypeDefinitionRegistry registry) {
        this.registry = registry;
    }

    public String toJavaType(Type<?> type) {
        if (type instanceof TypeName) {
            TypeName typeName = (TypeName) type;
            String javaName = typeName.getName();
            Optional<ScalarTypeDefinition> scalarType = registry.getType(javaName, ScalarTypeDefinition.class);
            if (scalarType.isPresent()) {
                javaName = mapScalarTypeName(scalarType.get());
            }

            return javaName;
        }
        if (type instanceof NonNullType) {
            NonNullType nonNullType = (NonNullType) type;
            String nullableType = toJavaType(nonNullType.getType());
            return NON_NULL_SCALARS.getOrDefault(nullableType, nullableType);
        }
        if (type instanceof ListType) {
            ListType listType = (ListType) type;
            return String.format("List<%s>", toJavaType(listType.getType()));
        }
        throw new IllegalArgumentException(type.getClass().getName());
    }

    public boolean isListType(Type<?> type) {
        if (type instanceof TypeName) {
            return false;
        }
        if (type instanceof NonNullType) {
            NonNullType nonNullType = (NonNullType) type;
            return isListType(nonNullType.getType());
        }
        if (type instanceof ListType) {
            return true;
        }
        throw new IllegalArgumentException(type.getClass().getName());
    }

    private String mapScalarTypeName(ScalarTypeDefinition scalarType) {
        String gqlName = scalarType.getName();
        String fallback = String.format("String /* %s */", gqlName);
        return BUILT_IN_SCALARS.getOrDefault(gqlName, fallback);
    }

    public String toJavaVariable(String name) {
        if (Constants.JAVA_KEYWORDS.contains(name)) {
            return "$" + name;
        }
        return name;
    }
}
