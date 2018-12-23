package org.ops4j.graqulus.cdi.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;

import org.ops4j.graqulus.cdi.api.BatchLoader;
import org.ops4j.graqulus.cdi.api.Query;
import org.ops4j.graqulus.cdi.api.Resolver;
import org.ops4j.graqulus.cdi.api.Schema;

public class GraqulusExtension implements Extension {

    private ClassScanResult scanResult = new ClassScanResult();

    <T> void processSchema(@Observes @WithAnnotations(Schema.class) ProcessAnnotatedType<T> pat) {
        Schema schema = pat.getAnnotatedType().getAnnotation(Schema.class);
        scanResult.setModelPackage(schema.modelPackage());
        scanResult.setSchemaPath(schema.path());
    }

    <T> void processQuery(@Observes @WithAnnotations(Query.class) ProcessAnnotatedType<T> pat) {
        for (AnnotatedMethod<?> method : pat.getAnnotatedType().getMethods()) {
            scanResult.registerQueryMethod(method);
        }
    }

    <T> void processBatchLoader(@Observes @WithAnnotations(BatchLoader.class) ProcessAnnotatedType<T> pat) {
        pat.getAnnotatedType().getMethods().stream()
            .filter(m -> m.getAnnotation(BatchLoader.class) != null)
            .forEach(scanResult::registerBatchLoaderMethod);
    }

    <T extends Resolver<?>> void processAnnotatedType(@Observes ProcessAnnotatedType<T> pat) {
        Set<Type> closure = pat.getAnnotatedType().getTypeClosure();
        Type resolver = closure.stream().filter(this::isResolver).findFirst().get();
        ParameterizedType paramType = (ParameterizedType) resolver;
        Class<?> gqlType = (Class<?>) paramType.getActualTypeArguments()[0];

        String typeName = gqlType.getSimpleName();
        scanResult.registerTypeResolver(typeName, pat.getAnnotatedType());
    }

    private boolean isResolver(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            if (paramType.getRawType() == Resolver.class) {
                return true;
            }
        }
        return false;
    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery) {
        afterBeanDiscovery.addBean()
                .addType(ClassScanResult.class)
                .scope(ApplicationScoped.class)
                .createWith(ctx -> scanResult);
    }

    void afterDeploymentValidation(@Observes AfterDeploymentValidation adv, BeanManager beanManager) {
        beanManager.createInstance().select(GraqulusExecutor.class).get().validateSchemaAndWiring();
    }
}
