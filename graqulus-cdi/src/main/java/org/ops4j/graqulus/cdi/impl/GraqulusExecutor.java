package org.ops4j.graqulus.cdi.impl;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;

import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.ops4j.graqulus.cdi.api.ExecutionRoot;
import org.ops4j.graqulus.cdi.api.ExecutionRootFactory;
import org.ops4j.graqulus.cdi.api.Schema;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeTraverser;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.UnExecutableSchemaGenerator;
import graphql.schema.idl.errors.SchemaProblem;

@ApplicationScoped
public class GraqulusExecutor implements ExecutionRootFactory {

    @Inject
    private ClassScanResult scanResult;

    @Inject
    private Instance<Object> instance;

    private TypeDefinitionRegistry registry;
    private RuntimeWiring runtimeWiring;
    private GraphQLSchema executableSchema;

    private List<Exception> deploymentProblems = new ArrayList<>();

    private Schema schemaAnnotation;

    public List<Exception> validateSchemaAndWiring() {
        findSchemaOnRootOperations();
        buildTypeDefinitionRegistry();
        buildRuntimeWiring();

        executableSchema = new SchemaGenerator().makeExecutableSchema(registry, runtimeWiring);

        EnumTypeLoader enumTypeLoader = new EnumTypeLoader(executableSchema, schemaAnnotation.modelPackage());
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

        schemaAnnotation = typesWithSchema.get(0).getAnnotation(Schema.class);
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
        String[] paths = schemaAnnotation.path();
        if (paths.length == 0) {
            throw new DefinitionException("@Schema annotation must contain at least one path argument");
        }
        TypeDefinitionRegistry mainRegistry = loadAndParseSchema(paths[0]);
        for (int i = 1; i < paths.length; i++) {
            TypeDefinitionRegistry additionalRegistry = loadAndParseSchema(paths[i]);
            mainRegistry.merge(additionalRegistry);
        }
        registry = mainRegistry;
        executableSchema = UnExecutableSchemaGenerator.makeUnExecutableSchema(registry);
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

    private void buildRuntimeWiring() {
        RuntimeWiringVisitor visitor = new RuntimeWiringVisitor(executableSchema, registry,
                schemaAnnotation.modelPackage());
        TypeTraverser traverser = new TypeTraverser();
        traverser.depthFirst(visitor, executableSchema.getAllTypesAsList());

        runtimeWiring = visitor.getRuntimeWiring();
    }

    private String toCommaList(List<AnnotatedType<?>> annotatedTypes) {
        return annotatedTypes.stream().map(t -> t.getJavaClass().getName()).collect(joining(", "));
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
