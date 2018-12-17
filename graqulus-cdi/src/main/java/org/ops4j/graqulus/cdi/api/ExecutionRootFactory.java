package org.ops4j.graqulus.cdi.api;

import graphql.GraphQL;

public interface ExecutionRootFactory {
    GraphQL newRoot();
}
