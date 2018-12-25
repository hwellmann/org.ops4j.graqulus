package org.ops4j.graqulus.generator.java;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.ops4j.graqulus.generator.util.FileHelper.createDirectoryIfNeeded;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.ops4j.graqulus.generator.trimou.TemplateEngine;

import graphql.language.Definition;
import graphql.language.Document;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeVisitor;
import graphql.schema.TypeTraverser;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.UnExecutableSchemaGenerator;

public class JavaGenerator {

    private JavaConfiguration config;

    private JavaContext context;

    private Document document;

    private TypeDefinitionRegistry registry;

    @SuppressWarnings("rawtypes")
    private List<Definition> definitions = new ArrayList<>();

    public JavaGenerator(JavaConfiguration config) {
        this.config = config;
        this.context = new JavaContext(config);
    }

    JavaContext getContext() {
        return context;
    }

    public void generate() throws IOException {
        loadSchema();

        String packagePath = config.getBasePackage().replace('.', '/');
        File packageDir = new File(config.getTargetDir(), packagePath);
        createDirectoryIfNeeded(packageDir);

        TemplateEngine templateEngine = new TemplateEngine();
        context.setPackageDir(packageDir);
        context.setRegistry(registry);
        context.setTemplateEngine(templateEngine);

        GraphQLSchema schema = UnExecutableSchemaGenerator.makeUnExecutableSchema(registry);

        GraphQLTypeVisitor visitor = new JavaGeneratingTypeVisitor(context);
        TypeTraverser traverser = new TypeTraverser();
        traverser.depthFirst(visitor, schema.getAllTypesAsList());
    }

    private void loadSchema() throws IOException {
        TypeDefinitionRegistry mainRegistry = loadAndParseSchema(config.getSourceFiles().get(0));
        for (int i = 1; i < config.getSourceFiles().size(); i++) {
            TypeDefinitionRegistry additionalRegistry = loadAndParseSchema(config.getSourceFiles().get(i));
            mainRegistry.merge(additionalRegistry);
        }
        registry = mainRegistry;
        document = new Document(definitions);
    }

    private TypeDefinitionRegistry loadAndParseSchema(String schemaPath) throws IOException {
        String schemaText = new String(Files.readAllBytes(Paths.get(schemaPath)), UTF_8);

        Parser parser = new Parser();
        document = parser.parseDocument(schemaText);
        definitions.addAll(document.getDefinitions());
        SchemaParser schemaParser = new SchemaParser();
        return schemaParser.buildRegistry(document);
    }
}
