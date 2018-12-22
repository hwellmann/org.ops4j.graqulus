package org.ops4j.graqulus.cdi.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.dataloader.DataLoader;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentBuilder;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.PropertyDataFetcher;

public class BatchListDataFetcher<T> implements DataFetcher<CompletionStage<List<T>>> {

    private PropertyDataFetcher<Object> idFetcher;

    public BatchListDataFetcher(String idProperty) {
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
        String fieldTypeName = GraphQLTypeUtil.unwrapAll(env.getFieldType()).getName();
        DataLoader<String, T> dataLoader = env.getDataLoader(fieldTypeName);
        return dataLoader.loadMany(ids);
    }

    private String toId(Object ref, DataFetchingEnvironment parentEnv) {
        DataFetchingEnvironment env = new DataFetchingEnvironmentBuilder().source(ref)
                .executionContext(parentEnv.getExecutionContext()).build();
        return idFetcher.get(env).toString();
    }
}
