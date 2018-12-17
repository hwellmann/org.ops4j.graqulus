package org.ops4j.graqulus.cdi.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.dataloader.BatchLoader;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentBuilder;
import graphql.schema.PropertyDataFetcher;

public class BatchListDataFetcher<T> implements DataFetcher<CompletionStage<List<T>>> {

    private BatchLoader<String, T> batchLoader;
    private PropertyDataFetcher<Object> idFetcher;

    public BatchListDataFetcher(BatchLoader<String, T> batchLoader, String idProperty) {
        this.batchLoader = batchLoader;
        this.idFetcher = new PropertyDataFetcher<>(idProperty);
    }

    @Override
    public CompletionStage<List<T>> get(DataFetchingEnvironment env) throws Exception {
        PropertyDataFetcher<Object> refsFetcher = PropertyDataFetcher.fetching(env.getField().getName());

        @SuppressWarnings("unchecked")
        List<Object> refs = (List<Object>) refsFetcher.get(env);
        if (refs == null) {
            return CompletableFuture.completedFuture(null);
        }

        List<String> ids = refs.stream().map(ref -> toId(ref, env)).collect(Collectors.toList());
        return batchLoader.load(ids);
    }

    private String toId(Object ref, DataFetchingEnvironment parentEnv) {
        DataFetchingEnvironment env = new DataFetchingEnvironmentBuilder().source(ref)
                .executionContext(parentEnv.getExecutionContext()).build();
        return idFetcher.get(env).toString();
    }
}
