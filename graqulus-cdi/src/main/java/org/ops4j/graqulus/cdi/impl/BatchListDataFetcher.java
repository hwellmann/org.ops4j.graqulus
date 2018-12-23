package org.ops4j.graqulus.cdi.impl;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private Method batchLoaderMethod;
    private MethodInvoker methodInvoker;

    public BatchListDataFetcher(Method batchLoaderMethod, String idProperty, MethodInvoker methodInvoker) {
        this.batchLoaderMethod = batchLoaderMethod;
        this.idFetcher = new PropertyDataFetcher<>(idProperty);
        this.methodInvoker = methodInvoker;
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

        if (batchLoaderMethod.getParameterCount() > 1) {
            List<Object> keyContexts = ids.stream().map(id -> buildKeyContext(env)).collect(toList());
            return dataLoader.loadMany(ids, keyContexts);
        }

        return dataLoader.loadMany(ids);
    }

    private Map<String, Object> buildKeyContext(DataFetchingEnvironment env) {
        Map<String, Object> keyContext = new HashMap<>();
        for (int i = 1; i < batchLoaderMethod.getParameterCount(); i++) {
            Parameter param = batchLoaderMethod.getParameters()[i];
            String paramName = param.getName();
            Object arg = methodInvoker.findArgumentOnStack(paramName, env);
            keyContext.put(paramName, arg);
        }
        return keyContext;
    }

    private String toId(Object ref, DataFetchingEnvironment parentEnv) {
        DataFetchingEnvironment env = new DataFetchingEnvironmentBuilder().source(ref)
                .executionContext(parentEnv.getExecutionContext()).build();
        return idFetcher.get(env).toString();
    }
}
