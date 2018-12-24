package org.ops4j.graqulus.cdi.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

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

    public Object invokeResolverMethod(Method resolverMethod, Resolver<?> resolver,
            DataFetchingEnvironment env)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Object args[] = getResolverInvocationArguments(resolverMethod, env);
        return resolverMethod.invoke(resolver, args);
    }

    private Object[] getInvocationArguments(AnnotatedMethod<?> queryMethod, DataFetchingEnvironment env) {
        Object[] args = new Object[queryMethod.getParameters().size()];
        int pos = 0;
        for (AnnotatedParameter<?> param : queryMethod.getParameters()) {
            args[pos] = getInvocationArgument(param.getJavaParameter(), env);
            pos++;
        }
        return args;
    }

    private Object[] getResolverInvocationArguments(Method queryMethod, DataFetchingEnvironment env) {
        Object[] args = new Object[queryMethod.getParameters().length];
        int pos = 0;
        for (Parameter param : queryMethod.getParameters()) {
            if (pos == 0) {
                args[0] = env.getSource();
            } else {
                args[pos] = getInvocationArgument(param, env);
            }
            pos++;
        }
        return args;
    }

    private Object getInvocationArgument(Parameter param, DataFetchingEnvironment env) {
        String paramName = param.getName();
        return findArgumentOnStack(paramName, env);
    }

    public <T> T findArgumentOnStack(String name, DataFetchingEnvironment env) {
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
