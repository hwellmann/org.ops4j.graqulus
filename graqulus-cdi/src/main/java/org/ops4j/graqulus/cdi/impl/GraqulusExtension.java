package org.ops4j.graqulus.cdi.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;

import org.ops4j.graqulus.cdi.api.ExecutionRootFactory;
import org.ops4j.graqulus.cdi.api.Query;
import org.ops4j.graqulus.cdi.api.Schema;

public class GraqulusExtension implements Extension {

    private GraqulusExecutor executor = new GraqulusExecutor();

    <T> void processSchema(@Observes @WithAnnotations(Schema.class) ProcessAnnotatedType<T> pat) {
        Schema schema = pat.getAnnotatedType().getAnnotation(Schema.class);
        executor.setModelPackage(schema.modelPackage());
        executor.setSchemaPath(schema.path());
    }

    <T> void processQuery(@Observes @WithAnnotations(Query.class) ProcessAnnotatedType<T> pat) {
        for (AnnotatedMethod<?> method : pat.getAnnotatedType().getMethods()) {
            executor.registerQueryMethod(method);
        }
    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        executor.setBeanManager(beanManager);
        executor.validateSchemaAndWiring();

        afterBeanDiscovery.addBean()
                .addType(ExecutionRootFactory.class)
                .scope(ApplicationScoped.class)
                .createWith(ctx -> executor);
    }
}
