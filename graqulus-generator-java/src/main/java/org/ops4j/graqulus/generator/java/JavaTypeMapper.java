package org.ops4j.graqulus.generator.java;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.Type;
import graphql.language.TypeName;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;

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

    public String toJavaType(GraphQLType type) {
        if (GraphQLTypeUtil.isNonNull(type)) {
            return toJavaType(GraphQLTypeUtil.unwrapOne(type));
        }
        if (GraphQLTypeUtil.isList(type)) {
            String itemType = toJavaType(GraphQLTypeUtil.unwrapOne(type));
            return String.format("List<%s>", itemType);
        }

        String javaName = type.getName();
        if (type instanceof GraphQLScalarType) {
            GraphQLScalarType scalarType = (GraphQLScalarType) type;
            javaName = mapScalarTypeName(scalarType);
        }

        return javaName;
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

    private String mapScalarTypeName(GraphQLScalarType scalarType) {
        Optional<String> javaClassName = getJavaClassName(scalarType);
        if (javaClassName.isPresent()) {
            return javaClassName.get();
        }

        String gqlName = scalarType.getName();
        String fallback = String.format("String /* %s */", gqlName);
        return BUILT_IN_SCALARS.getOrDefault(gqlName, fallback);
    }

    private Optional<String> getJavaClassName(GraphQLScalarType scalarType) {
        GraphQLDirective directive = scalarType.getDirective("javaClass");
        if (directive == null) {
            return Optional.empty();
        }
        String directiveValue = (String) directive.getArgument("name").getValue();
        return Optional.of(directiveValue);
    }

    public String toJavaVariable(String name) {
        if (Constants.JAVA_KEYWORDS.contains(name)) {
            return "$" + name;
        }
        return name;
    }
}
