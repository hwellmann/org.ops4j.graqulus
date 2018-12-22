package org.ops4j.graqulus.cdi.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.dataloader.DataLoader;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentBuilder;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.PropertyDataFetcher;

public class BatchDataFetcher<T> implements DataFetcher<CompletionStage<T>> {

    private PropertyDataFetcher<Object> idFetcher;

    public BatchDataFetcher(String idProperty) {
        this.idFetcher = new PropertyDataFetcher<>(idProperty);
    }

    @Override
    public CompletionStage<T> get(DataFetchingEnvironment env) throws Exception {
        PropertyDataFetcher<Object> refsFetcher = PropertyDataFetcher.fetching(env.getField().getName());
        Object ref = refsFetcher.get(env);
        if (ref == null) {
            return CompletableFuture.completedFuture(null);
        }
        String id = toId(ref, env);
        String fieldTypeName = GraphQLTypeUtil.unwrapAll(env.getFieldType()).getName();
        DataLoader<String, T> dataLoader = env.getDataLoader(fieldTypeName);
        return dataLoader.load(id);
    }

    private String toId(Object ref, DataFetchingEnvironment parentEnv) {
        DataFetchingEnvironment env = new DataFetchingEnvironmentBuilder().source(ref)
                .executionContext(parentEnv.getExecutionContext()).build();
        return idFetcher.get(env).toString();
    }
}
