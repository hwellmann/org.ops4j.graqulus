package org.ops4j.graqulus.cdi.impl;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.BatchLoaderWithContext;

import io.earcam.unexceptional.Exceptional;

public class AsyncBatchLoaderWithContext<T> implements BatchLoaderWithContext<String, T> {

    private Object service;
    private Method method;

    public AsyncBatchLoaderWithContext(Object service, Method method) {
        this.service = service;
        this.method = method;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletionStage<List<T>> load(List<String> keys, BatchLoaderEnvironment environment) {
        Map<Object, Object> keyContexts = environment.getKeyContexts();

        int numParams = method.getParameterCount();
        Object[] args = new Object[numParams];
        args[0] = keys;
        for (int i = 1; i < numParams; i++) {
            String paramName = method.getParameters()[i].getName();
            Function<String, Object> arg = key -> getContextObject(key, keyContexts, paramName);
            args[i] = arg;
        }
        if (CompletionStage.class.isAssignableFrom(method.getReturnType())) {
        	return (CompletionStage<List<T>>) Exceptional.call(() -> method.invoke(service, args));
        }
        return CompletableFuture.supplyAsync(Exceptional.uncheckSupplier(() -> (List<T>) method.invoke(service, args)));
    }


    private Object getContextObject(String key, Map<Object, Object> keyContexts, String paramName) {

        @SuppressWarnings("unchecked")
        Map<String, Object> argsForKey = (Map<String, Object>) keyContexts.get(key);

        if (argsForKey == null) {
            return null;
        }
        return argsForKey.get(paramName);
    }
}
