package org.ops4j.graqulus.shared;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import graphql.language.ObjectTypeDefinition;
import graphql.language.OperationTypeDefinition;
import graphql.language.SchemaDefinition;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeInfo;

public class OperationTypeRegistry {

    public static final String QUERY = "Query";
    public static final String MUTATION = "Mutation";
    public static final String SUBSCRIPTION = "Subscription";

    private TypeDefinitionRegistry registry;
    private Map<String, String> operationTypeMap = new HashMap<>();

    public OperationTypeRegistry(TypeDefinitionRegistry registry) {
        this.registry = registry;
        buildOperationTypes();
    }

    private void buildOperationTypes() {
        operationTypeMap.put(QUERY.toLowerCase(), QUERY);
        operationTypeMap.put(MUTATION.toLowerCase(), MUTATION);
        operationTypeMap.put(SUBSCRIPTION.toLowerCase(), SUBSCRIPTION);
        Optional<SchemaDefinition> optSchemaDefinition = registry.schemaDefinition();
        if (optSchemaDefinition.isPresent()) {
            for (OperationTypeDefinition op : optSchemaDefinition.get().getOperationTypeDefinitions()) {
                String name = op.getName();
                String typeName = TypeInfo.typeInfo(op.getType()).getName();
                operationTypeMap.put(name, typeName);
            }
        }
    }

    public boolean isOperationType(ObjectTypeDefinition objectType) {
        return operationTypeMap.values().contains(objectType.getName());
    }
}
