package org.ops4j.graqulus.cdi.impl;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLScalarType.newScalar;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;

import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.ops4j.graqulus.cdi.api.ExecutionRoot;
import org.ops4j.graqulus.cdi.api.ExecutionRootFactory;
import org.ops4j.graqulus.cdi.api.IdPropertyStrategy;
import org.ops4j.graqulus.cdi.api.Resolver;
import org.ops4j.graqulus.shared.OperationTypeRegistry;

import graphql.GraphQL;
import graphql.TypeResolutionEnvironment;
import graphql.execution.ExecutionStepInfo;
import graphql.language.FieldDefinition;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.ScalarTypeDefinition;
import graphql.language.TypeDefinition;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.RuntimeWiring.Builder;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeInfo;
import graphql.schema.idl.TypeRuntimeWiring;

@ApplicationScoped
public class GraqulusExecutor implements ExecutionRootFactory {

    @Inject
    private ClassScanResult scanResult;

    @Inject
    private Instance<Object> instance;

    @Inject
    private IdPropertyStrategy idPropertyStrategy;

    private TypeDefinitionRegistry registry;
    private RuntimeWiring runtimeWiring;
    private GraphQLSchema executableSchema;
    private OperationTypeRegistry operationTypeRegistry;

    public void validateSchemaAndWiring() {
        loadAndParseSchema();
        buildRuntimeWiring();

        executableSchema = new SchemaGenerator().makeExecutableSchema(registry, runtimeWiring);
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

    private void buildRuntimeWiring() {
        Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring();

        addInterfaceTypes(runtimeWiringBuilder);
        addObjectTypes(runtimeWiringBuilder);
        addScalarTypes(runtimeWiringBuilder);

        runtimeWiring = runtimeWiringBuilder.build();
    }

    private void addInterfaceTypes(Builder runtimeWiringBuilder) {
        for (InterfaceTypeDefinition interfaceType : registry.getTypes(InterfaceTypeDefinition.class)) {
            TypeRuntimeWiring.Builder interfaceWiringBuilder = newTypeWiring(interfaceType.getName())
                    .typeResolver(this::resolveInterface);
            runtimeWiringBuilder.type(interfaceWiringBuilder);
        }
    }

    private void addObjectTypes(Builder runtimeWiringBuilder) {
        for (ObjectTypeDefinition objectType : registry.getTypes(ObjectTypeDefinition.class)) {
            if (operationTypeRegistry.isOperationType(objectType)) {
                runtimeWiringBuilder.type(buildOperationTypeWiring(objectType));
            } else {
                addObjectTypeWiring(runtimeWiringBuilder, objectType);
            }
        }
    }

    private void addScalarTypes(Builder runtimeWiringBuilder) {
        for (ScalarTypeDefinition scalarType : registry.scalars().values()) {
            addScalarType(runtimeWiringBuilder, scalarType);
        }
    }

    private void addScalarType(Builder runtimeWiringBuilder, ScalarTypeDefinition scalarType) {
        if (scalarType.getSourceLocation() != null) {
            runtimeWiringBuilder.scalar(newScalar(GraphQLString).name(scalarType.getName()).build());
        }
    }

    private TypeRuntimeWiring.Builder buildOperationTypeWiring(ObjectTypeDefinition objectType) {
        TypeRuntimeWiring.Builder queryWiringBuilder = TypeRuntimeWiring.newTypeWiring(objectType.getName());
        for (FieldDefinition query : objectType.getFieldDefinitions()) {
            DataFetcher<?> dataFetcher = buildDataFetcher(query);
            queryWiringBuilder.dataFetcher(query.getName(), dataFetcher);
        }
        return queryWiringBuilder;
    }

    private void addObjectTypeWiring(RuntimeWiring.Builder runtimeWiringBuilder, ObjectTypeDefinition objectType) {
        AnnotatedType<?> typeResolver = scanResult.getTypeResolver(objectType.getName());
        if (typeResolver == null) {
            return;
        }

        TypeRuntimeWiring.Builder objectTypeWiringBuilder = newTypeWiring(objectType.getName());
        Resolver<?> resolver = (Resolver<?>) instance.select(typeResolver.getJavaClass()).get();

        for (FieldDefinition fieldDef : objectType.getFieldDefinitions()) {
            addFieldDataFetcher(objectTypeWiringBuilder, fieldDef, resolver, typeResolver);
        }

        runtimeWiringBuilder.type(objectTypeWiringBuilder);
    }

    private <T> void addFieldDataFetcher(TypeRuntimeWiring.Builder objectTypeWiringBuilder, FieldDefinition fieldDef,
            Resolver<?> resolver, AnnotatedType<T> resolverType) {
        String fieldName = fieldDef.getName();
        Optional<AnnotatedMethod<? super T>> method = resolverType.getMethods().stream()
                .filter(m -> m.getJavaMember().getName().equals(fieldName)).findFirst();
        if (method.isPresent()) {
            DataFetcher<?> dataFetcher = buildFieldResolverDataFetcher(resolver, method.get());
            objectTypeWiringBuilder.dataFetcher(fieldDef.getName(), dataFetcher);
            return;
        }

        boolean loadAllById = resolver.loadAllById();
        List<String> loadById = resolver.loadById();
        if (requiresDataFetcher(fieldDef.getType()) && (loadAllById || loadById.contains(fieldName))) {
            DataFetcher<?> dataFetcher = buildDataFetcher(fieldDef.getType(), resolver);
            objectTypeWiringBuilder.dataFetcher(fieldDef.getName(), dataFetcher);
        }
    }

    private DataFetcher<?> buildFieldResolverDataFetcher(Resolver<?> resolver,
            AnnotatedMethod<?> resolverMethod) {
        return env -> invokeResolverMethod(resolverMethod, resolver, env);
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

    private DataFetcher<?> buildDataFetcher(graphql.language.Type<?> type, Resolver<?> resolver) {
        TypeInfo typeInfo = TypeInfo.typeInfo(type);
        TypeDefinition<?> typeDef = registry.getType(type).get();
        AnnotatedMethod<?> batchLoaderMethod = scanResult.getBatchLoaderMethod(typeDef.getName());
        if (batchLoaderMethod == null) {
            throw new DeploymentException("No batch loader for type " + typeDef.getName());
        }

        String idProperty = idPropertyStrategy.id(typeDef.getName());
        if (typeInfo.isList()) {
            return new BatchListDataFetcher<>(idProperty);
        } else {
            return new BatchDataFetcher<>(idProperty);
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
        return env -> invokeQueryMethod(queryMethod, env);
    }

    private Object invokeQueryMethod(AnnotatedMethod<?> queryMethod, DataFetchingEnvironment env)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Object service = instance.select(queryMethod.getDeclaringType().getJavaClass()).get();
        Object args[] = getInvocationArguments(queryMethod, env);
        return queryMethod.getJavaMember().invoke(service, args);
    }

    private Object invokeResolverMethod(AnnotatedMethod<?> resolverMethod, Resolver<?> resolver,
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

    @Override
    public ExecutionRoot newRoot() {
        GraphQL root = GraphQL.newGraphQL(executableSchema).build();
        return new ExecutionRootImpl(root, buildDataLoaderRegistry());
    }

    private DataLoaderRegistry buildDataLoaderRegistry() {
        DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
        scanResult.getBatchLoaderMethods().forEach((type, method) ->
            dataLoaderRegistry.register(type, buildDataLoader(method)));
        return dataLoaderRegistry;
    }

    private DataLoader<?, ?> buildDataLoader(AnnotatedMethod<?> method) {
        Object service = instance.select(method.getDeclaringType().getJavaClass()).get();
        AsyncBatchLoader<Object> batchLoader = new AsyncBatchLoader<>(service, method.getJavaMember());
        return DataLoader.newDataLoader(batchLoader);
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
