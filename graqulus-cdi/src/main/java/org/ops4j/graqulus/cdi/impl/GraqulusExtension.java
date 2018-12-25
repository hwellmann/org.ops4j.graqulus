package org.ops4j.graqulus.cdi.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.WithAnnotations;

import org.ops4j.graqulus.cdi.api.BatchLoader;
import org.ops4j.graqulus.cdi.api.Resolver;
import org.ops4j.graqulus.cdi.api.RootOperation;
import org.ops4j.graqulus.cdi.api.Serializer;

public class GraqulusExtension implements Extension {

    private ClassScanResult scanResult = new ClassScanResult();

    <T> void processQuery(@Observes @WithAnnotations(RootOperation.class) ProcessAnnotatedType<T> event) {
        scanResult.registerRootOperation(event.getAnnotatedType());
    }

    <T> void processBatchLoader(@Observes @WithAnnotations(BatchLoader.class) ProcessAnnotatedType<T> event) {
        event.getAnnotatedType().getMethods().stream()
                .filter(m -> m.getAnnotation(BatchLoader.class) != null)
                .forEach(scanResult::registerBatchLoaderMethod);
    }

    <T extends Resolver<?>> void processResolver(@Observes ProcessBean<T> event) {
        Set<Type> closure = event.getBean().getTypes();
        Type resolver = closure.stream().filter(this::isResolver).findFirst().get();
        ParameterizedType paramType = (ParameterizedType) resolver;
        Class<?> gqlType = (Class<?>) paramType.getActualTypeArguments()[0];

        String typeName = gqlType.getSimpleName();
        scanResult.registerTypeResolver(typeName, event.getBean());
    }

    <T extends Serializer<?, ?>> void processSerializer(@Observes ProcessBean<T> event) {
        Set<Type> closure = event.getBean().getTypes();
        Type serializer = closure.stream().filter(this::isSerializer).findFirst().get();
        ParameterizedType paramType = (ParameterizedType) serializer;
        Class<?> objectType = (Class<?>) paramType.getActualTypeArguments()[0];

        String javaClassName = objectType.getName();
        scanResult.registerSerializer(javaClassName, event.getBean());
    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        event.addBean()
                .addType(ClassScanResult.class)
                .scope(ApplicationScoped.class)
                .createWith(ctx -> scanResult);
    }

    void afterDeploymentValidation(@Observes AfterDeploymentValidation event, BeanManager beanManager) {
        GraqulusExecutor executor = beanManager.createInstance().select(GraqulusExecutor.class).get();
        List<Exception> deploymentProblems = executor.validateSchemaAndWiring();
        deploymentProblems.stream().forEach(event::addDeploymentProblem);
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

    private boolean isSerializer(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            if (paramType.getRawType() == Serializer.class) {
                return true;
            }
        }
        return false;
    }

}
