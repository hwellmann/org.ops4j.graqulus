package org.ops4j.graqulus.cdi.impl;

import org.ops4j.graqulus.shared.OperationTypeRegistry;

import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.schema.GraphQLUnionType;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.RuntimeWiring.Builder;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

public class RuntimeWiringVisitor extends GraphQLTypeVisitorStub {

    private String modelPackage;
    private Builder runtimeWiringBuilder;
    private OperationTypeRegistry operationTypeRegistry;
    private TypeDefinitionRegistry registry;
    private GraphQLSchema schema;

    public RuntimeWiringVisitor(GraphQLSchema schema, TypeDefinitionRegistry registry, String modelPackage) {
        this.schema = schema;
        this.modelPackage = modelPackage;
        this.registry = registry;
        this.runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring();
        this.operationTypeRegistry = new OperationTypeRegistry(schema);
    }

    public RuntimeWiring getRuntimeWiring() {
        return runtimeWiringBuilder.build();
    }

    @Override
    public TraversalControl visitGraphQLObjectType(GraphQLObjectType objectType,
            TraverserContext<GraphQLType> context) {
        if (operationTypeRegistry.isOperationType(objectType.getName())) {
            OperationTypeRuntimeWiringVisitor operationTypeVisitor
                = new OperationTypeRuntimeWiringVisitor(runtimeWiringBuilder, schema, registry, modelPackage);
            operationTypeVisitor.addOperationType(objectType);
        } else {
            ObjectTypeRuntimeWiringVisitor objectTypeVisitor = new ObjectTypeRuntimeWiringVisitor(runtimeWiringBuilder);
            objectTypeVisitor.addObjectType(objectType);
        }
        return TraversalControl.CONTINUE;
    }

    @Override
    public TraversalControl visitGraphQLInterfaceType(GraphQLInterfaceType interfaceType,
            TraverserContext<GraphQLType> context) {
        new InterfaceTypeRuntimeWiringVisitor(runtimeWiringBuilder).addInterfaceType(interfaceType);
        return TraversalControl.CONTINUE;
    }

    @Override
    public TraversalControl visitGraphQLUnionType(GraphQLUnionType unionType, TraverserContext<GraphQLType> context) {
        new UnionTypeRuntimeWiringVisitor(runtimeWiringBuilder).addUnionType(unionType);
        return TraversalControl.CONTINUE;
    }

    @Override
    public TraversalControl visitGraphQLScalarType(GraphQLScalarType scalarType,
            TraverserContext<GraphQLType> context) {
        ScalarTypeRuntimeWiringVisitor scalarTypeVisitor = new ScalarTypeRuntimeWiringVisitor(registry,
                runtimeWiringBuilder);
        scalarTypeVisitor.addScalarType(scalarType);
        return TraversalControl.CONTINUE;
    }
}
