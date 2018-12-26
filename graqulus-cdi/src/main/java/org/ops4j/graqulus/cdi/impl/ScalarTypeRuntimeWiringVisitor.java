package org.ops4j.graqulus.cdi.impl;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLScalarType.newScalar;

import java.util.Optional;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.CDI;

import org.ops4j.graqulus.cdi.api.Serializer;

import graphql.language.ScalarTypeDefinition;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring.Builder;
import graphql.schema.idl.TypeDefinitionRegistry;

public class ScalarTypeRuntimeWiringVisitor {

    private Builder runtimeWiringBuilder;
    private ClassScanResult scanResult;
    private TypeDefinitionRegistry registry;

    public ScalarTypeRuntimeWiringVisitor(TypeDefinitionRegistry registry, Builder runtimeWiringBuilder) {
        this.registry = registry;
        this.runtimeWiringBuilder = runtimeWiringBuilder;
        this.scanResult = CDI.current().select(ClassScanResult.class).get();
    }

    public void addScalarType(GraphQLScalarType scalarType) {
        if (isBuiltIn(scalarType)) {
            return;
        }

        Optional<String> javaClassName = getJavaClassName(scalarType);
        if (javaClassName.isPresent()) {
            Bean<?> serializerBean = scanResult.getSerializer(javaClassName.get());
            if (serializerBean != null) {
                Serializer<?, ?> serializer = (Serializer<?, ?>) CDI.current().select(serializerBean.getBeanClass())
                        .get();
                CoercingWrapper<?, ?> wrapper = new CoercingWrapper<>(serializer);
                GraphQLScalarType scalarTypeWrapper = newScalar().name(scalarType.getName()).coercing(wrapper).build();
                runtimeWiringBuilder.scalar(scalarTypeWrapper).build();
                return;
            }
        }

        runtimeWiringBuilder.scalar(newScalar(GraphQLString).name(scalarType.getName()).build());
    }

    private boolean isBuiltIn(GraphQLScalarType scalarType) {
        ScalarTypeDefinition scalarTypeDef = registry.getType(scalarType.getName(), ScalarTypeDefinition.class).get();
        return scalarTypeDef.getSourceLocation() == null;
    }

    private Optional<String> getJavaClassName(GraphQLScalarType scalarType) {
        GraphQLDirective directive = scalarType.getDirective("javaClass");
        if (directive == null) {
            return Optional.empty();
        }
        String directiveValue = (String) directive.getArgument("name").getValue();
        return Optional.of(directiveValue);
    }
}
