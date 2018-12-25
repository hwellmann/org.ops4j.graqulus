package org.ops4j.graqulus.cdi.impl;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLScalarType.newScalar;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;

import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.ops4j.graqulus.cdi.api.ExecutionRoot;
import org.ops4j.graqulus.cdi.api.ExecutionRootFactory;
import org.ops4j.graqulus.cdi.api.IdPropertyStrategy;
import org.ops4j.graqulus.cdi.api.Resolver;
import org.ops4j.graqulus.cdi.api.Schema;
import org.ops4j.graqulus.cdi.api.Serializer;
import org.ops4j.graqulus.shared.OperationTypeRegistry;
import org.ops4j.graqulus.shared.ReflectionHelper;

import graphql.GraphQL;
import graphql.TypeResolutionEnvironment;
import graphql.language.Directive;
import graphql.language.FieldDefinition;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.ScalarTypeDefinition;
import graphql.language.StringValue;
import graphql.language.TypeDefinition;
import graphql.language.UnionTypeDefinition;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.RuntimeWiring.Builder;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeInfo;
import graphql.schema.idl.TypeRuntimeWiring;
import graphql.schema.idl.errors.SchemaProblem;

@ApplicationScoped
public class GraqulusExecutor implements ExecutionRootFactory {

    @Inject
    private ClassScanResult scanResult;

    @Inject
    private Instance<Object> instance;

    @Inject
    private IdPropertyStrategy idPropertyStrategy;

    @Inject
    private MethodInvoker methodInvoker;

    private TypeDefinitionRegistry registry;
    private RuntimeWiring runtimeWiring;
    private GraphQLSchema executableSchema;
    private OperationTypeRegistry operationTypeRegistry;

    private List<Exception> deploymentProblems = new ArrayList<>();

    private Schema schema;

    public List<Exception> validateSchemaAndWiring() {
        findSchemaOnRootOperations();
        buildTypeDefinitionRegistry();
        checkOperationRoots();
        buildRuntimeWiring();

        executableSchema = new SchemaGenerator().makeExecutableSchema(registry, runtimeWiring);

        EnumTypeLoader enumTypeLoader = new EnumTypeLoader(registry, executableSchema, schema.modelPackage());
        enumTypeLoader.overrideEnumerationValues();

        return deploymentProblems;
    }

    private void findSchemaOnRootOperations() {
        List<AnnotatedType<?>> typesWithSchema = scanResult.getRootOperations().stream()
                .filter(this::isResolvable)
                .filter(this::hasSchema)
                .collect(toList());

        if (typesWithSchema.isEmpty()) {
            throw new DeploymentException("No @Schema annotation found on root operation beans");
        }

        if (typesWithSchema.size() > 1) {
            String classes = toCommaList(typesWithSchema);
            throw new DefinitionException("Multiple @Schema annotations found on classes " + classes);
        }

        schema = typesWithSchema.get(0).getAnnotation(Schema.class);
    }

    private boolean isResolvable(AnnotatedType<?> annotatedType) {
        return instance.select(annotatedType.getJavaClass()).isResolvable();
    }

    private boolean hasSchema(AnnotatedType<?> annotatedType) {
        return annotatedType.isAnnotationPresent(Schema.class);
    }

    @Override
    public ExecutionRoot newRoot() {
        GraphQL root = GraphQL.newGraphQL(executableSchema).build();
        return new ExecutionRootImpl(root, buildDataLoaderRegistry());
    }

    private void buildTypeDefinitionRegistry() {
        String[] paths = schema.path();
        if (paths.length == 0) {
            throw new DefinitionException("@Schema annotation must contain at least one path argument");
        }
        TypeDefinitionRegistry mainRegistry = loadAndParseSchema(paths[0]);
        for (int i = 1; i < paths.length; i++) {
            TypeDefinitionRegistry additionalRegistry = loadAndParseSchema(paths[i]);
            mainRegistry.merge(additionalRegistry);
        }
        registry = mainRegistry;
    }

    private TypeDefinitionRegistry loadAndParseSchema(String schemaPath) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        InputStream is = tccl.getResourceAsStream(schemaPath);
        if (is == null) {
            throw new DefinitionException("No schema resource with path " + schemaPath);
        }

        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            SchemaParser parser = new SchemaParser();
            return parser.parse(reader);
        } catch (IOException exc) {
            throw new DefinitionException("Cannot read schema resource", exc);
        } catch (SchemaProblem exc) {
            throw new DefinitionException("Invalid schema", exc);
        }
    }

    private void checkOperationRoots() {
        operationTypeRegistry = new OperationTypeRegistry(registry);
    }

    private void buildRuntimeWiring() {
        Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring();

        addInterfaceTypes(runtimeWiringBuilder);
        addUnionTypes(runtimeWiringBuilder);
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

    private void addUnionTypes(Builder runtimeWiringBuilder) {
        for (UnionTypeDefinition unionType : registry.getTypes(UnionTypeDefinition.class)) {
            TypeRuntimeWiring.Builder unionWiringBuilder = newTypeWiring(unionType.getName())
                    .typeResolver(this::resolveUnion);
            runtimeWiringBuilder.type(unionWiringBuilder);
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

    private void addScalarType(Builder runtimeWiringBuilder, ScalarTypeDefinition scalarTypeDef) {
        if (isBuiltIn(scalarTypeDef)) {
            return;
        }

        Optional<String> javaClassName = getJavaClassName(scalarTypeDef);
        if (javaClassName.isPresent()) {
            Bean<?> serializerBean = scanResult.getSerializer(javaClassName.get());
            if (serializerBean != null) {
                Serializer<?, ?> serializer =
                        (Serializer<?, ?>) instance.select(serializerBean.getBeanClass()).get();
                CoercingWrapper<?, ?> wrapper = new CoercingWrapper<>(serializer);
                GraphQLScalarType scalarType = newScalar().name(scalarTypeDef.getName()).coercing(wrapper).build();
                runtimeWiringBuilder.scalar(scalarType).build();
            }
        }
        runtimeWiringBuilder.scalar(newScalar(GraphQLString).name(scalarTypeDef.getName()).build());
    }

    private Optional<String> getJavaClassName(ScalarTypeDefinition scalarType) {
        Directive directive = scalarType.getDirective("javaClass");
        if (directive == null) {
            return Optional.empty();
        }
        StringValue directiveValue = (StringValue) directive.getArgument("name").getValue();
        return Optional.of(directiveValue.getValue());
    }

    private boolean isBuiltIn(ScalarTypeDefinition scalarType) {
        return scalarType.getSourceLocation() == null;
    }

    private TypeRuntimeWiring.Builder buildOperationTypeWiring(ObjectTypeDefinition objectType) {
        String typeName = objectType.getName();
        String interfaceName = String.format("%s.%s", schema.modelPackage(), typeName);

        List<AnnotatedType<?>> annotatedTypes = scanResult.getRootOperations().stream()
                .filter(this::isResolvable)
                .filter(t -> hasInterface(t, interfaceName))
                .collect(toList());

        if (annotatedTypes.isEmpty()) {
            throw new DeploymentException("There is no @RootOperation bean implementing " + interfaceName);
        }

        if (annotatedTypes.size() > 1) {
            String classes = toCommaList(annotatedTypes);
            throw new DeploymentException("There are multiple @RootOperation beans implementing "
                    + interfaceName + ": " + classes);
        }

        AnnotatedType<?> operationType = annotatedTypes.get(0);

        TypeRuntimeWiring.Builder queryWiringBuilder = TypeRuntimeWiring.newTypeWiring(objectType.getName());
        for (FieldDefinition query : objectType.getFieldDefinitions()) {
            DataFetcher<?> dataFetcher = buildDataFetcher(query, operationType);
            queryWiringBuilder.dataFetcher(query.getName(), dataFetcher);
        }
        return queryWiringBuilder;
    }

    private String toCommaList(List<AnnotatedType<?>> annotatedTypes) {
        return annotatedTypes.stream().map(t -> t.getJavaClass().getName()).collect(joining(", "));
    }

    private boolean hasInterface(AnnotatedType<?> annotatedType, String interfaceName) {
        return Stream.of(annotatedType.getJavaClass().getInterfaces()).anyMatch(i -> i.getName().equals(interfaceName));
    }

    private void addObjectTypeWiring(RuntimeWiring.Builder runtimeWiringBuilder, ObjectTypeDefinition objectType) {
        Bean<?> typeResolver = scanResult.getTypeResolver(objectType.getName());
        if (typeResolver == null) {
            return;
        }

        TypeRuntimeWiring.Builder objectTypeWiringBuilder = newTypeWiring(objectType.getName());
        Resolver<?> resolver = (Resolver<?>) instance.select(typeResolver.getBeanClass()).get();

        for (FieldDefinition fieldDef : objectType.getFieldDefinitions()) {
            addFieldDataFetcher(objectTypeWiringBuilder, fieldDef, resolver, typeResolver);
        }

        runtimeWiringBuilder.type(objectTypeWiringBuilder);
    }

    private <T> void addFieldDataFetcher(TypeRuntimeWiring.Builder objectTypeWiringBuilder, FieldDefinition fieldDef,
            Resolver<?> resolver, Bean<T> resolverType) {

        Optional<Method> method = findResolverMethodForField(resolverType, fieldDef);

        if (method.isPresent()) {
            DataFetcher<?> dataFetcher = buildFieldResolverDataFetcher(resolver, method.get());
            objectTypeWiringBuilder.dataFetcher(fieldDef.getName(), dataFetcher);
        } else {
            addFieldDataFetcherWithBatchLoader(objectTypeWiringBuilder, fieldDef, resolver);
        }
    }

    private Optional<Method> findResolverMethodForField(Bean<?> resolverType, FieldDefinition fieldDef) {
        String fieldName = fieldDef.getName();
        return Stream.of(resolverType.getBeanClass().getMethods())
                .filter(m -> m.getName().equals(fieldName))
                .findFirst();
    }

    private <T> void addFieldDataFetcherWithBatchLoader(TypeRuntimeWiring.Builder objectTypeWiringBuilder,
            FieldDefinition fieldDef, Resolver<?> resolver) {
        boolean loadAllById = resolver.loadAllById();
        List<String> loadById = resolver.loadById();
        if (requiresDataFetcher(fieldDef.getType()) && (loadAllById || loadById.contains(fieldDef.getName()))) {
            DataFetcher<?> dataFetcher = buildDataFetcher(fieldDef.getType(), resolver);
            objectTypeWiringBuilder.dataFetcher(fieldDef.getName(), dataFetcher);
        }
    }


    private DataFetcher<?> buildFieldResolverDataFetcher(Resolver<?> resolver, Method resolverMethod) {
        return env -> methodInvoker.invokeResolverMethod(resolverMethod, resolver, env);
    }

    private boolean requiresDataFetcher(graphql.language.Type<?> type) {
        TypeDefinition<?> typeDef = registry.getType(type).get();
        if (typeDef instanceof InterfaceTypeDefinition) {
            return true;
        }
        if (typeDef instanceof ObjectTypeDefinition) {
            return true;
        }
        if (typeDef instanceof UnionTypeDefinition) {
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
            return new BatchListDataFetcher<>(batchLoaderMethod.getJavaMember(), idProperty, methodInvoker);
        } else {
            return new BatchDataFetcher<>(batchLoaderMethod.getJavaMember(), idProperty, methodInvoker);
        }
    }

    private GraphQLObjectType resolveInterface(TypeResolutionEnvironment env) {
        String typeName = env.getObject().getClass().getSimpleName();
        return env.getSchema().getObjectType(typeName);
    }

    private GraphQLObjectType resolveUnion(TypeResolutionEnvironment env) {
        String typeName = ReflectionHelper.invokeMethod(env.getObject(), "type");
        return env.getSchema().getObjectType(typeName);
    }

    private DataFetcher<?> buildDataFetcher(FieldDefinition query, AnnotatedType<?> operationType) {
        List<AnnotatedMethod<?>> queryMethods = operationType.getMethods().stream()
                .filter(m -> m.getJavaMember().getName().equals(query.getName()))
                .collect(toList());

        if (queryMethods.isEmpty()) {
            throw new DeploymentException(String.format("Class %s has no method named %s",
                    operationType.getJavaClass().getName(), query.getName()));
        }

        if (queryMethods.size() > 1) {
            throw new DeploymentException(String.format("Class %s has multiple methods named %s",
                    operationType.getJavaClass().getName(), query.getName()));
        }

        return env -> methodInvoker.invokeQueryMethod(queryMethods.get(0), env);
    }

    private DataLoaderRegistry buildDataLoaderRegistry() {
        DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
        scanResult.getBatchLoaderMethods().forEach((type, method) ->
            dataLoaderRegistry.register(type, buildDataLoader(method)));
        return dataLoaderRegistry;
    }

    private DataLoader<?, ?> buildDataLoader(AnnotatedMethod<?> method) {
        Object service = instance.select(method.getDeclaringType().getJavaClass()).get();
        if (method.getParameters().size() > 1) {
            return DataLoader.newDataLoader(new AsyncBatchLoaderWithContext<>(service, method.getJavaMember()));
        } else {
            return DataLoader.newDataLoader(new AsyncBatchLoader<>(service, method.getJavaMember()));
        }
    }


}
