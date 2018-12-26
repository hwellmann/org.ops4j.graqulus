package org.ops4j.graqulus.cdi.impl;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.CDI;

import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.ops4j.graqulus.cdi.api.ExecutionRoot;
import org.ops4j.graqulus.cdi.api.ExecutionRootFactory;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;

public class ExecutionRootFactoryImpl implements ExecutionRootFactory {

    private GraphQLSchema schema;

    public ExecutionRootFactoryImpl(GraphQLSchema schema) {
        this.schema = schema;
    }

    @Override
    public ExecutionRoot newRoot() {
        GraphQL root = GraphQL.newGraphQL(schema).build();
        return new ExecutionRootImpl(root, buildDataLoaderRegistry());
    }

    private DataLoaderRegistry buildDataLoaderRegistry() {
        DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
        ClassScanResult scanResult = CDI.current().select(ClassScanResult.class).get();
        scanResult.getBatchLoaderMethods()
                .forEach((type, method) -> dataLoaderRegistry.register(type, buildDataLoader(method)));
        return dataLoaderRegistry;
    }

    private DataLoader<?, ?> buildDataLoader(AnnotatedMethod<?> method) {
        Object service = CDI.current().select(method.getDeclaringType().getJavaClass()).get();
        if (method.getParameters().size() > 1) {
            return DataLoader.newDataLoader(new AsyncBatchLoaderWithContext<>(service, method.getJavaMember()));
        } else {
            return DataLoader.newDataLoader(new AsyncBatchLoader<>(service, method.getJavaMember()));
        }
    }
}
