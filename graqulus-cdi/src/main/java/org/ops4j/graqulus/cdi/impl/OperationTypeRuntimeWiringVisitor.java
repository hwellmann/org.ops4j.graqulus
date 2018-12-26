package org.ops4j.graqulus.cdi.impl;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Stream;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.DeploymentException;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.schema.idl.RuntimeWiring.Builder;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;

public class OperationTypeRuntimeWiringVisitor extends GraphQLTypeVisitorStub {

    private String modelPackage;
    private Builder runtimeWiringBuilder;
    private MethodInvoker methodInvoker;
    private ClassScanResult scanResult;

    public OperationTypeRuntimeWiringVisitor(Builder runtimeWiringBuilder, GraphQLSchema schema,
            TypeDefinitionRegistry registry, String modelPackage) {
        this.modelPackage = modelPackage;
        this.runtimeWiringBuilder = runtimeWiringBuilder;
        this.methodInvoker = CDI.current().select(MethodInvoker.class).get();
        this.scanResult = CDI.current().select(ClassScanResult.class).get();
    }

    public void addOperationType(GraphQLObjectType objectType) {
        String typeName = objectType.getName();
        String interfaceName = String.format("%s.%s", modelPackage, typeName);

        List<AnnotatedType<?>> annotatedTypes = scanResult.getRootOperations().stream()
                .filter(t -> isResolvable(t))
                .filter(t -> hasInterface(t, interfaceName))
                .collect(toList());

        if (annotatedTypes.isEmpty()) {
            throw new DeploymentException("There is no @RootOperation bean implementing " + interfaceName);
        }

        if (annotatedTypes.size() > 1) {
            String classes = toCommaList(annotatedTypes);
            throw new DeploymentException("There are multiple @RootOperation beans implementing "
                    + interfaceName + ": " + classes);
        }

        AnnotatedType<?> operationType = annotatedTypes.get(0);

        TypeRuntimeWiring.Builder queryWiringBuilder = TypeRuntimeWiring.newTypeWiring(objectType.getName());
        for (GraphQLFieldDefinition query : objectType.getFieldDefinitions()) {
            DataFetcher<?> dataFetcher = buildDataFetcher(query, operationType);
            queryWiringBuilder.dataFetcher(query.getName(), dataFetcher);
        }

        runtimeWiringBuilder.type(queryWiringBuilder);
    }

    private boolean isResolvable(AnnotatedType<?> annotatedType) {
        return CDI.current().select(annotatedType.getJavaClass()).isResolvable();
    }

    private boolean hasInterface(AnnotatedType<?> annotatedType, String interfaceName) {
        return Stream.of(annotatedType.getJavaClass().getInterfaces()).anyMatch(i -> i.getName().equals(interfaceName));
    }

    private String toCommaList(List<AnnotatedType<?>> annotatedTypes) {
        return annotatedTypes.stream().map(t -> t.getJavaClass().getName()).collect(joining(", "));
    }

    private DataFetcher<?> buildDataFetcher(GraphQLFieldDefinition query, AnnotatedType<?> operationType) {
        List<AnnotatedMethod<?>> queryMethods = operationType.getMethods().stream()
                .filter(m -> m.getJavaMember().getName().equals(query.getName()))
                .collect(toList());

        if (queryMethods.isEmpty()) {
            throw new DeploymentException(String.format("Class %s has no method named %s",
                    operationType.getJavaClass().getName(), query.getName()));
        }

        if (queryMethods.size() > 1) {
            throw new DeploymentException(String.format("Class %s has multiple methods named %s",
                    operationType.getJavaClass().getName(), query.getName()));
        }

        return env -> methodInvoker.invokeQueryMethod(queryMethods.get(0), env);
    }
}
