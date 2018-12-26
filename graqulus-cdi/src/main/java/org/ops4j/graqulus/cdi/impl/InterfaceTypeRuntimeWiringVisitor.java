package org.ops4j.graqulus.cdi.impl;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.schema.idl.RuntimeWiring.Builder;
import graphql.schema.idl.TypeRuntimeWiring;

public class InterfaceTypeRuntimeWiringVisitor extends GraphQLTypeVisitorStub {

    private Builder runtimeWiringBuilder;

    public InterfaceTypeRuntimeWiringVisitor(Builder runtimeWiringBuilder) {
        this.runtimeWiringBuilder = runtimeWiringBuilder;
    }

    public void addInterfaceType(GraphQLInterfaceType interfaceType) {
        TypeRuntimeWiring.Builder interfaceWiringBuilder = newTypeWiring(interfaceType.getName())
                .typeResolver(this::resolveInterface);
        runtimeWiringBuilder.type(interfaceWiringBuilder);
    }

    private GraphQLObjectType resolveInterface(TypeResolutionEnvironment env) {
        String typeName = env.getObject().getClass().getSimpleName();
        return env.getSchema().getObjectType(typeName);
    }
}
