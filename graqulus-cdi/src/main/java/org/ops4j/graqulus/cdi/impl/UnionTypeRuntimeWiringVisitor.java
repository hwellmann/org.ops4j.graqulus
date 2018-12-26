package org.ops4j.graqulus.cdi.impl;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

import org.ops4j.graqulus.shared.ReflectionHelper;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.schema.GraphQLUnionType;
import graphql.schema.idl.RuntimeWiring.Builder;
import graphql.schema.idl.TypeRuntimeWiring;

public class UnionTypeRuntimeWiringVisitor extends GraphQLTypeVisitorStub {

    private Builder runtimeWiringBuilder;

    public UnionTypeRuntimeWiringVisitor(Builder runtimeWiringBuilder) {
        this.runtimeWiringBuilder = runtimeWiringBuilder;
    }

    public void addUnionType(GraphQLUnionType unionType) {
        TypeRuntimeWiring.Builder unionWiringBuilder = newTypeWiring(unionType.getName())
                .typeResolver(this::resolveUnion);
        runtimeWiringBuilder.type(unionWiringBuilder);
    }

    private GraphQLObjectType resolveUnion(TypeResolutionEnvironment env) {
        String typeName = ReflectionHelper.invokeMethod(env.getObject(), "type");
        return env.getSchema().getObjectType(typeName);
    }
}
