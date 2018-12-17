package org.ops4j.graqulus.cdi.impl;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.dataloader.BatchLoader;

import io.earcam.unexceptional.Exceptional;

public class AsyncBatchLoader<T> implements BatchLoader<String, T> {

    private Object service;
    private Method method;

    public AsyncBatchLoader(Object service, Method method) {
        this.service = service;
        this.method = method;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletionStage<List<T>> load(List<String> keys) {
        return CompletableFuture.supplyAsync(Exceptional.uncheckSupplier(() -> (List<T>) method.invoke(service, keys)));
    }
}
