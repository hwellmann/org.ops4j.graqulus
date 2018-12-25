package org.ops4j.graqulus.shared;

import java.util.Objects;
import java.util.stream.Stream;

import graphql.schema.GraphQLSchema;

public class OperationTypeRegistry {

    public static final String QUERY = "Query";
    public static final String MUTATION = "Mutation";
    public static final String SUBSCRIPTION = "Subscription";

    private GraphQLSchema schema;

    public OperationTypeRegistry(GraphQLSchema schema) {
        this.schema = schema;
    }

    public boolean isOperationType(String objectTypeName) {
        return Stream.of(schema.getQueryType(), schema.getMutationType(), schema.getSubscriptionType())
                .filter(Objects::nonNull)
                .anyMatch(t -> t.getName().equals(objectTypeName));
    }
}
