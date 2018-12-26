package org.ops4j.graqulus.cdi.impl;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.DeploymentException;

import org.ops4j.graqulus.cdi.api.IdPropertyStrategy;
import org.ops4j.graqulus.cdi.api.Resolver;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnionType;
import graphql.schema.idl.RuntimeWiring.Builder;
import graphql.schema.idl.TypeRuntimeWiring;

public class ObjectTypeRuntimeWiringVisitor {

    private Builder runtimeWiringBuilder;
    private MethodInvoker methodInvoker;
    private ClassScanResult scanResult;
    private IdPropertyStrategy idPropertyStrategy;

    public ObjectTypeRuntimeWiringVisitor(Builder runtimeWiringBuilder) {
        this.runtimeWiringBuilder = runtimeWiringBuilder;
        this.methodInvoker = CDI.current().select(MethodInvoker.class).get();
        this.scanResult = CDI.current().select(ClassScanResult.class).get();
        this.idPropertyStrategy = CDI.current().select(IdPropertyStrategy.class).get();
    }

    public void addObjectType(GraphQLObjectType objectType) {
        Bean<?> typeResolver = scanResult.getTypeResolver(objectType.getName());
        if (typeResolver == null) {
            return;
        }

        TypeRuntimeWiring.Builder objectTypeWiringBuilder = newTypeWiring(objectType.getName());
        Resolver<?> resolver = (Resolver<?>) CDI.current().select(typeResolver.getBeanClass()).get();

        for (GraphQLFieldDefinition fieldDef : objectType.getFieldDefinitions()) {
            addFieldDataFetcher(objectTypeWiringBuilder, fieldDef, resolver, typeResolver);
        }

        runtimeWiringBuilder.type(objectTypeWiringBuilder);
    }

    private <T> void addFieldDataFetcher(TypeRuntimeWiring.Builder objectTypeWiringBuilder,
            GraphQLFieldDefinition fieldDef,
            Resolver<?> resolver, Bean<T> resolverType) {

        Optional<Method> method = findResolverMethodForField(resolverType, fieldDef);

        if (method.isPresent()) {
            DataFetcher<?> dataFetcher = buildFieldResolverDataFetcher(resolver, method.get());
            objectTypeWiringBuilder.dataFetcher(fieldDef.getName(), dataFetcher);
        } else {
            addFieldDataFetcherWithBatchLoader(objectTypeWiringBuilder, fieldDef, resolver);
        }
    }

    private Optional<Method> findResolverMethodForField(Bean<?> resolverType, GraphQLFieldDefinition fieldDef) {
        String fieldName = fieldDef.getName();
        return Stream.of(resolverType.getBeanClass().getMethods())
                .filter(m -> m.getName().equals(fieldName))
                .findFirst();
    }

    private <T> void addFieldDataFetcherWithBatchLoader(TypeRuntimeWiring.Builder objectTypeWiringBuilder,
            GraphQLFieldDefinition fieldDef, Resolver<?> resolver) {
        boolean loadAllById = resolver.loadAllById();
        List<String> loadById = resolver.loadById();
        if (requiresDataFetcher(fieldDef) && (loadAllById || loadById.contains(fieldDef.getName()))) {
            DataFetcher<?> dataFetcher = buildDataFetcher(fieldDef, resolver);
            objectTypeWiringBuilder.dataFetcher(fieldDef.getName(), dataFetcher);
        }
    }

    private DataFetcher<?> buildFieldResolverDataFetcher(Resolver<?> resolver, Method resolverMethod) {
        return env -> methodInvoker.invokeResolverMethod(resolverMethod, resolver, env);
    }

    private boolean requiresDataFetcher(GraphQLFieldDefinition fieldDef) {
        GraphQLOutputType typeDef = fieldDef.getType();
        if (typeDef instanceof GraphQLInterfaceType) {
            return true;
        }
        if (typeDef instanceof GraphQLObjectType) {
            return true;
        }
        if (typeDef instanceof GraphQLUnionType) {
            return true;
        }
        return false;
    }

    private DataFetcher<?> buildDataFetcher(GraphQLFieldDefinition fieldDef, Resolver<?> resolver) {
        String typeName = fieldDef.getType().getName();
        AnnotatedMethod<?> batchLoaderMethod = scanResult.getBatchLoaderMethod(typeName);
        if (batchLoaderMethod == null) {
            throw new DeploymentException("No batch loader for type " + typeName);
        }

        String idProperty = idPropertyStrategy.id(typeName);
        if (GraphQLTypeUtil.isList(fieldDef.getType())) {
            return new BatchListDataFetcher<>(batchLoaderMethod.getJavaMember(), idProperty, methodInvoker);
        } else {
            return new BatchDataFetcher<>(batchLoaderMethod.getJavaMember(), idProperty, methodInvoker);
        }
    }
}
