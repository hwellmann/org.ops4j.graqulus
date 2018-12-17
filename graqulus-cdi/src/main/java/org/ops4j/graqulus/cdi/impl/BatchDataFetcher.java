package org.ops4j.graqulus.cdi.impl;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.dataloader.BatchLoader;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentBuilder;
import graphql.schema.PropertyDataFetcher;

public class BatchDataFetcher<T> implements DataFetcher<CompletionStage<T>> {

    private BatchLoader<String, T> batchLoader;
    private PropertyDataFetcher<Object> idFetcher;

    public BatchDataFetcher(BatchLoader<String, T> batchLoader, String idProperty) {
        this.batchLoader = batchLoader;
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
        return batchLoader.load(Collections.singletonList(id)).thenApply(list -> list.get(0));
    }

    private String toId(Object ref, DataFetchingEnvironment parentEnv) {
        DataFetchingEnvironment env = new DataFetchingEnvironmentBuilder().source(ref)
                .executionContext(parentEnv.getExecutionContext()).build();
        return idFetcher.get(env).toString();
    }
}
