package org.ops4j.graqulus.cdi.impl;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.BeanManager;

import org.dataloader.BatchLoader;

import io.earcam.unexceptional.Exceptional;

public class AsyncBatchLoader<T> implements BatchLoader<String, T> {

    private Object service;
    private AnnotatedMethod<?> loaderMethod;
    private BeanManager beanManager;
    private Method method;

    public AsyncBatchLoader(BeanManager beanManager, AnnotatedMethod<?> loaderMethod) {
        this.beanManager = beanManager;
        this.loaderMethod = loaderMethod;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletionStage<List<T>> load(List<String> keys) {
        if (service == null) {
            service = beanManager.createInstance().select(loaderMethod.getDeclaringType().getJavaClass()).get();
            method = loaderMethod.getJavaMember();
        }
        return CompletableFuture.supplyAsync(Exceptional.uncheckSupplier(() -> (List<T>) method.invoke(service, keys)));
    }
}
