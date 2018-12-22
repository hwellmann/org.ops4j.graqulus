package org.ops4j.graqulus.cdi.impl;

import java.util.Map;

import org.dataloader.DataLoaderRegistry;
import org.ops4j.graqulus.cdi.api.ExecutionRoot;

import graphql.ExecutionInput;
import graphql.ExecutionInput.Builder;
import graphql.ExecutionResult;
import graphql.GraphQL;

public class ExecutionRootImpl implements ExecutionRoot {

    private Builder builder;
    private DataLoaderRegistry dataLoaderRegistry;
    private GraphQL root;

    public ExecutionRootImpl(GraphQL root, DataLoaderRegistry dataLoaderRegistry) {
        this.root = root;
        this.dataLoaderRegistry = dataLoaderRegistry;
        this.builder = ExecutionInput.newExecutionInput();
    }

    @Override
    public ExecutionRoot query(String query) {
        builder.query(query);
        return this;
    }

    @Override
    public ExecutionRoot operationName(String operationName) {
        builder.operationName(operationName);
        return this;
    }

    @Override
    public ExecutionRoot variables(Map<String, Object> variables) {
        builder.variables(variables);
        return this;
    }

    @Override
    public ExecutionResult execute() {
        ExecutionInput executionInput = builder.dataLoaderRegistry(dataLoaderRegistry).build();
        return root.execute(executionInput);
    }

    @Override
    public ExecutionResult execute(String query) {
        return query(query).execute();
    }
}
