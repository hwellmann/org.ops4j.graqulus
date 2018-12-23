package org.ops4j.graqulus.cdi.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
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
    private Method batchLoaderMethod;
    private MethodInvoker methodInvoker;

    public BatchDataFetcher(Method batchLoaderMethod, String idProperty, MethodInvoker methodInvoker) {
        this.batchLoaderMethod = batchLoaderMethod;
        this.idFetcher = new PropertyDataFetcher<>(idProperty);
        this.methodInvoker = methodInvoker;
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

        int numParams = batchLoaderMethod.getParameters().length;
        if (numParams > 1) {
            Map<String, Object> keyContext = new HashMap<>();
            for (int i = 1; i < numParams; i++) {
                Parameter param = batchLoaderMethod.getParameters()[i];
                String paramName = param.getName();
                Object arg = methodInvoker.findArgumentOnStack(paramName, env);
                keyContext.put(paramName, arg);
            }
            dataLoader.load(id, keyContext);
        }
        return dataLoader.load(id);
    }

    private String toId(Object ref, DataFetchingEnvironment parentEnv) {
        DataFetchingEnvironment env = new DataFetchingEnvironmentBuilder().source(ref)
                .executionContext(parentEnv.getExecutionContext()).build();
        return idFetcher.get(env).toString();
    }
}
