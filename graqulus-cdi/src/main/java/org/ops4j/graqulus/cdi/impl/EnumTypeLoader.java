package org.ops4j.graqulus.cdi.impl;

import static org.ops4j.graqulus.shared.ReflectionHelper.readField;
import static org.ops4j.graqulus.shared.ReflectionHelper.writeField;

import java.util.Map;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import io.earcam.unexceptional.Exceptional;

public class EnumTypeLoader {

    private static final String VALUE_DEFINITION_MAP = "valueDefinitionMap";

    private GraphQLSchema executableSchema;
    private String modelPackage;

    public EnumTypeLoader(GraphQLSchema executableSchema, String modelPackage) {
        this.executableSchema = executableSchema;
        this.modelPackage = modelPackage;
    }

    public void overrideEnumerationValues() {
        executableSchema.getAllTypesAsList().stream()
            .filter(t -> t instanceof GraphQLEnumType)
            .forEach(this::overrideEnumValues);
    }

    private void overrideEnumValues(GraphQLType type) {
        GraphQLEnumType origEnumType = (GraphQLEnumType) type;
        if (origEnumType.getDefinition() == null) {
            return;
        }
        GraphQLEnumType copiedEnumType = buildCopyWithJavaEnumValue(origEnumType);

        Map<String, GraphQLEnumValueDefinition> valueDefinitionMap = readField(copiedEnumType, VALUE_DEFINITION_MAP);

        writeField(origEnumType, VALUE_DEFINITION_MAP, valueDefinitionMap);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private GraphQLEnumType buildCopyWithJavaEnumValue(GraphQLEnumType origEnumType) {
        GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum().name(origEnumType.getName());
        Class<? extends Enum> enumClass = loadEnumClass(origEnumType.getName());
        for (GraphQLEnumValueDefinition value : origEnumType.getValues()) {
            builder.value(value.getName(), Enum.valueOf(enumClass, value.getName()));
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private <T extends Enum<T>> Class<T> loadEnumClass(String enumTypeName) {
        String className = String.format("%s.%s", modelPackage, enumTypeName);
        try {
            return (Class<T>) Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException exc) {
            throw Exceptional.throwAsUnchecked(exc);
        }
    }
}
