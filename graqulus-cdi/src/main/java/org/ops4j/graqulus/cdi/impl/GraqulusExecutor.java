package org.ops4j.graqulus.cdi.impl;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;

import org.ops4j.graqulus.cdi.api.ExecutionRootFactory;
import org.ops4j.graqulus.shared.OperationTypeRegistry;

import graphql.GraphQL;
import graphql.TypeResolutionEnvironment;
import graphql.language.FieldDefinition;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeDefinition;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.RuntimeWiring.Builder;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeInfo;
import graphql.schema.idl.TypeRuntimeWiring;
import io.earcam.unexceptional.Exceptional;

@ApplicationScoped
public class GraqulusExecutor implements ExecutionRootFactory {

    @Inject
    private BeanManager beanManager;

    @Inject
    private ClassScanResult scanResult;

    private TypeDefinitionRegistry registry;
    private RuntimeWiring runtimeWiring;
    private GraphQLSchema executableSchema;

    private OperationTypeRegistry operationTypeRegistry;

    public void validateSchemaAndWiring() {
        loadAndParseSchema();
        buildWiring();

        executableSchema = new SchemaGenerator().makeExecutableSchema(registry, runtimeWiring);
    }

    private void buildWiring() {
        Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring();

        for (InterfaceTypeDefinition interfaceType : registry.getTypes(InterfaceTypeDefinition.class)) {
            TypeRuntimeWiring.Builder interfaceWiringBuilder = newTypeWiring(interfaceType.getName())
                    .typeResolver(this::resolveInterface);
            runtimeWiringBuilder.type(interfaceWiringBuilder);
        }

        for (ObjectTypeDefinition objectType : registry.getTypes(ObjectTypeDefinition.class)) {
            if (operationTypeRegistry.isOperationType(objectType)) {
                runtimeWiringBuilder.type(buildOperationTypeWiring(objectType));
            } else {
                addObjectTypeWiring(runtimeWiringBuilder, objectType);
            }
        }

        runtimeWiring = runtimeWiringBuilder.build();
    }

    private void addObjectTypeWiring(Builder runtimeWiringBuilder, ObjectTypeDefinition objectType) {
        TypeRuntimeWiring.Builder objectTypeWiringBuilder = newTypeWiring(objectType.getName());
        boolean requiresFetcher = false;
        for (FieldDefinition fieldDef : objectType.getFieldDefinitions()) {
            if (requiresDataFetcher(fieldDef.getType())) {
                requiresFetcher = true;
                DataFetcher<?> dataFetcher = buildDataFetcher(fieldDef.getType());
                objectTypeWiringBuilder.dataFetcher(fieldDef.getName(), dataFetcher);
            }
        }
        if (requiresFetcher) {
            runtimeWiringBuilder.type(objectTypeWiringBuilder);
        }
    }

    private graphql.schema.idl.TypeRuntimeWiring.Builder buildOperationTypeWiring(ObjectTypeDefinition objectType) {
        TypeRuntimeWiring.Builder queryWiringBuilder = TypeRuntimeWiring.newTypeWiring(objectType.getName());
        for (FieldDefinition query : objectType.getFieldDefinitions()) {
            DataFetcher<?> dataFetcher = buildDataFetcher(query);
            queryWiringBuilder.dataFetcher(query.getName(), dataFetcher);
        }
        return queryWiringBuilder;
    }

    private boolean requiresDataFetcher(graphql.language.Type<?> type) {
        TypeDefinition<?> typeDef = registry.getType(type).get();
        if (typeDef instanceof InterfaceTypeDefinition) {
            return true;
        }
        if (typeDef instanceof ObjectTypeDefinition) {
            return true;
        }
        return false;
    }

    private DataFetcher<?> buildDataFetcher(graphql.language.Type<?> type) {
        TypeInfo typeInfo = TypeInfo.typeInfo(type);
        TypeDefinition<?> typeDef = registry.getType(type).get();
        AnnotatedMethod<?> batchLoaderMethod = scanResult.getBatchLoaderMethod(typeDef.getName());
        if (batchLoaderMethod == null) {
            throw new DeploymentException("No batch loader for type " + typeDef.getName());
        }
        AsyncBatchLoader<Object> batchLoader = new AsyncBatchLoader<>(beanManager, batchLoaderMethod);
        if (typeInfo.isList()) {
            return new BatchListDataFetcher<>(batchLoader, beanManager);
        } else {
            return new BatchDataFetcher<>(batchLoader, "id");
        }
    }

    private GraphQLObjectType resolveInterface(TypeResolutionEnvironment env) {
        String typeName = env.getObject().getClass().getSimpleName();
        return env.getSchema().getObjectType(typeName);
    }

    private DataFetcher<?> buildDataFetcher(FieldDefinition query) {
        AnnotatedMethod<?> queryMethod = scanResult.getQueryMethod(query.getName());
        if (queryMethod == null) {
            throw new DeploymentException("No query method for " + query.getName());
        }
        validateParameters(query, queryMethod);
        return env -> invokeQueryMethod(queryMethod, env);
    }

    private void validateParameters(FieldDefinition query, AnnotatedMethod<?> queryMethod) {
        // TODO Auto-generated method stub

    }

    private Object invokeQueryMethod(AnnotatedMethod<?> queryMethod, DataFetchingEnvironment env)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Instance<Object> instance = beanManager.createInstance();
        Object service = instance.select(queryMethod.getDeclaringType().getJavaClass()).get();
        Object args[] = getInvocationArguments(queryMethod, env);
        return queryMethod.getJavaMember().invoke(service, args);
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

    private Object getInvocationArgument(AnnotatedParameter<?> param, DataFetchingEnvironment env) {
        String paramName = param.getJavaParameter().getName();
        Object rawArg = env.getArgument(paramName);
        Object arg = rawArg;
        GraphQLArgument queryArg = env.getFieldDefinition().getArgument(paramName);
        if (queryArg.getType() instanceof GraphQLEnumType) {
            GraphQLEnumType enumType = (GraphQLEnumType) queryArg.getType();
            arg = convertEnumValue(enumType, (String) rawArg);
        }
        return arg;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object convertEnumValue(GraphQLEnumType enumType, String valueAsString) {
        if (valueAsString == null) {
            return null;
        }
        String javaClassName = String.format("%s.%s", scanResult.getModelPackage(), enumType.getName());
        try {
            Class enumClass = Thread.currentThread().getContextClassLoader().loadClass(javaClassName);
            return Enum.valueOf(enumClass, valueAsString);
        } catch (ClassNotFoundException exc) {
            throw Exceptional.throwAsUnchecked(exc);
        }
    }

    private void loadAndParseSchema() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String schemaPath = scanResult.getSchemaPath();
        InputStream is = tccl.getResourceAsStream(schemaPath);
        if (is == null) {
            throw new DefinitionException("No schema resource with path " + schemaPath);
        }

        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            SchemaParser parser = new SchemaParser();
            registry = parser.parse(reader);
            operationTypeRegistry = new OperationTypeRegistry(registry);
        } catch (IOException exc) {
            throw new DefinitionException(exc);
        }
    }

    @Override
    public GraphQL newRoot() {
        return GraphQL.newGraphQL(executableSchema).build();
    }
}
