package org.ops4j.graqulus.cdi.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.BeanManager;

import org.dataloader.BatchLoader;
import org.ops4j.graqulus.cdi.api.IdPropertyStrategy;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentBuilder;
import graphql.schema.PropertyDataFetcher;

public class BatchListDataFetcher<T> implements DataFetcher<CompletionStage<List<T>>> {

    private BatchLoader<String, T> batchLoader;
    private PropertyDataFetcher<Object> idFetcher;
    private BeanManager beanManager;

    public BatchListDataFetcher(BatchLoader<String, T> batchLoader, BeanManager beanManager) {
        this.batchLoader = batchLoader;
        this.beanManager = beanManager;
    }

    @Override
    public CompletionStage<List<T>> get(DataFetchingEnvironment env) throws Exception {
        if (idFetcher == null) {
            IdPropertyStrategy idPropertyStrategy = beanManager.createInstance().select(IdPropertyStrategy.class).get();
            String id = idPropertyStrategy.id(env.getFieldType().getName());
            idFetcher = new PropertyDataFetcher<>(id);
        }
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
