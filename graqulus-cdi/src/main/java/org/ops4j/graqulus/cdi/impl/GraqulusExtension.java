package org.ops4j.graqulus.cdi.impl;

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
        for (AnnotatedMethod<?> method : pat.getAnnotatedType().getMethods()) {
            scanResult.registerBatchLoaderMethod(method);
        }
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
