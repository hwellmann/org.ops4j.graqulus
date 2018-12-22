package org.ops4j.graqulus.cdi.impl;

import java.lang.reflect.InvocationTargetException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.inject.Inject;

import org.ops4j.graqulus.cdi.api.Resolver;

import graphql.execution.ExecutionStepInfo;
import graphql.schema.DataFetchingEnvironment;

@ApplicationScoped
public class MethodInvoker {

    @Inject
    private Instance<Object> instance;

    public Object invokeQueryMethod(GraqulusExecutor graqulusExecutor, AnnotatedMethod<?> queryMethod,
            DataFetchingEnvironment env)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Object service = instance.select(queryMethod.getDeclaringType().getJavaClass()).get();
        Object args[] = getInvocationArguments(queryMethod, env);
        return queryMethod.getJavaMember().invoke(service, args);
    }

    public Object invokeResolverMethod(AnnotatedMethod<?> resolverMethod, Resolver<?> resolver,
            DataFetchingEnvironment env)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Object args[] = getResolverInvocationArguments(resolverMethod, env);
        return resolverMethod.getJavaMember().invoke(resolver, args);
    }

    private Object[] getInvocationArguments(AnnotatedMethod<?> queryMethod, DataFetchingEnvironment env) {
        Object[] args = new Object[queryMethod.getParameters().size()];
        int pos = 0;
        for (AnnotatedParameter<?> param : queryMethod.getParameters()) {
            args[pos] = getInvocationArgument(param, env);
            pos++;
        }
        return args;
    }

    private Object[] getResolverInvocationArguments(AnnotatedMethod<?> queryMethod, DataFetchingEnvironment env) {
        Object[] args = new Object[queryMethod.getParameters().size()];
        int pos = 0;
        for (AnnotatedParameter<?> param : queryMethod.getParameters()) {
            if (pos == 0) {
                args[0] = env.getSource();
            } else {
                args[pos] = getInvocationArgument(param, env);
            }
            pos++;
        }
        return args;
    }

    private Object getInvocationArgument(AnnotatedParameter<?> param, DataFetchingEnvironment env) {
        String paramName = param.getJavaParameter().getName();
        Object arg = findArgumentOnStack(paramName, env);
        return maybeConvertEnumValue(param, arg);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object maybeConvertEnumValue(AnnotatedParameter<?> param, Object arg) {
        if (arg == null) {
            return null;
        }
        Class paramClass = param.getJavaParameter().getType();
        if (Enum.class.isAssignableFrom(paramClass)) {
            return Enum.valueOf(paramClass, arg.toString());

        }
        return arg;
    }

    private <T> T findArgumentOnStack(String name, DataFetchingEnvironment env) {
        T arg = env.getArgument(name);
        if (arg == null) {
            ExecutionStepInfo parent = env.getExecutionStepInfo().getParent();
            if (parent != null) {
                arg = findArgumentOnStack(name, parent);
            }
        }
        return arg;
    }

    private <T> T findArgumentOnStack(String name, ExecutionStepInfo info) {
        T arg = info.getArgument(name);
        if (arg == null) {
            if (info.getParent() != null) {
                arg = findArgumentOnStack(name, info.getParent());
            }
        }
        return arg;
    }

}
