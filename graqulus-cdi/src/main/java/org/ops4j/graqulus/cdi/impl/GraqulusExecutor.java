package org.ops4j.graqulus.cdi.impl;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;

import org.ops4j.graqulus.cdi.api.ExecutionRootFactory;

import graphql.GraphQL;
import graphql.TypeResolutionEnvironment;
import graphql.language.FieldDefinition;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.OperationTypeDefinition;
import graphql.language.SchemaDefinition;
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

@Vetoed
public class GraqulusExecutor implements ExecutionRootFactory {

    public static final String QUERY = "Query";
    public static final String MUTATION = "Mutation";
    public static final String SUBSCRIPTION = "Subscription";

    private String schemaPath;
    private String modelPackage;
    private Map<String, AnnotatedMethod<?>> queryMethodMap = new HashMap<>();
    private Map<String, AnnotatedMethod<?>> batchLoaderMap = new HashMap<>();
    private TypeDefinitionRegistry registry;
    private RuntimeWiring runtimeWiring;
    private GraphQLSchema executableSchema;

    private BeanManager beanManager;
    private Map<String, String> operationTypeMap;

    public String getSchemaPath() {
        return schemaPath;
    }

    public void setSchemaPath(String schemaPath) {
        this.schemaPath = schemaPath;
    }

    public String getModelPackage() {
        return modelPackage;
    }

    public void setModelPackage(String modelPackage) {
        this.modelPackage = modelPackage;
    }

    public void registerQueryMethod(AnnotatedMethod<?> method) {
        AnnotatedMethod<?> previous = queryMethodMap.putIfAbsent(method.getJavaMember().getName(), method);
        if (previous != null) {
            throw new DefinitionException("duplicate query method");
        }
    }

    public void registerBatchLoaderMethod(AnnotatedMethod<?> method) {
        Type returnType = method.getJavaMember().getGenericReturnType();
        if (returnType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) returnType;
            if (!paramType.getRawType().getTypeName().equals(List.class.getName())) {
                throw new DefinitionException("batch loader method must return java.util.List<T>");
            }
            Type itemType = paramType.getActualTypeArguments()[0];
            Class<?> itemClass = (Class<?>) itemType;

            AnnotatedMethod<?> previous = batchLoaderMap.putIfAbsent(itemClass.getSimpleName(), method);
            if (previous != null) {
                throw new DefinitionException("duplicate batch loader method");
            }
        }
    }

    public void setBeanManager(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    public void validateSchemaAndWiring() {
        loadAndParseSchema();
        buildOperationTypes();
        buildWiring();

        executableSchema = new SchemaGenerator().makeExecutableSchema(registry, runtimeWiring);
    }

    private void buildOperationTypes() {
        operationTypeMap = new HashMap<>();
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

    private void buildWiring() {
        Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring();

        for (InterfaceTypeDefinition interfaceType : registry.getTypes(InterfaceTypeDefinition.class)) {
            TypeRuntimeWiring.Builder interfaceWiringBuilder = newTypeWiring(interfaceType.getName())
                    .typeResolver(this::resolveInterface);
            runtimeWiringBuilder.type(interfaceWiringBuilder);
        }

        for (ObjectTypeDefinition objectType : registry.getTypes(ObjectTypeDefinition.class)) {
            if (operationTypeMap.values().contains(objectType.getName())) {
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
        TypeDefinition<?> typeDef = registry.getType(type).get();
        AnnotatedMethod<?> batchLoaderMethod = batchLoaderMap.get(typeDef.getName());
        if (batchLoaderMethod == null) {
            throw new DeploymentException("No batch loader for type " + typeDef.getName());
        }
        AsyncBatchLoader<Object> batchLoader = new AsyncBatchLoader<>(beanManager, batchLoaderMethod);
        return new BatchListDataFetcher<>(batchLoader);
    }

    private GraphQLObjectType resolveInterface(TypeResolutionEnvironment env) {
        String typeName = env.getObject().getClass().getSimpleName();
        return env.getSchema().getObjectType(typeName);
    }

    private DataFetcher<?> buildDataFetcher(FieldDefinition query) {
        AnnotatedMethod<?> queryMethod = queryMethodMap.get(query.getName());
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
        String javaClassName = String.format("%s.%s", modelPackage, enumType.getName());
        try {
            Class enumClass = Thread.currentThread().getContextClassLoader().loadClass(javaClassName);
            return Enum.valueOf(enumClass, valueAsString);
        } catch (ClassNotFoundException exc) {
            throw Exceptional.throwAsUnchecked(exc);
        }
    }

    private void loadAndParseSchema() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        InputStream is = tccl.getResourceAsStream(schemaPath);
        if (is == null) {
            throw new DefinitionException("No schema resource with path " + schemaPath);
        }

        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            SchemaParser parser = new SchemaParser();
            registry = parser.parse(reader);
        } catch (IOException exc) {
            throw new DefinitionException(exc);
        }
    }

    @Override
    public GraphQL newRoot() {
        return GraphQL.newGraphQL(executableSchema).build();
    }
}
