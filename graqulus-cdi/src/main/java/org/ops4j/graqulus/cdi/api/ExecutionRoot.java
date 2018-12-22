package org.ops4j.graqulus.cdi.api;

import java.util.Map;

import graphql.ExecutionResult;

public interface ExecutionRoot {

    ExecutionRoot query(String query);

    ExecutionRoot operationName(String operationName);

    ExecutionRoot variables(Map<String, Object> variables);

    ExecutionResult execute();

    ExecutionResult execute(String query);
}
