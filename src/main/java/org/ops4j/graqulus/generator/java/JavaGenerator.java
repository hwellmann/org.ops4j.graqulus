package org.ops4j.graqulus.generator.java;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.ops4j.graqulus.generator.util.FileHelper.createDirectoryIfNeeded;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.ops4j.graqulus.generator.trimou.TemplateEngine;

import graphql.language.Document;
import graphql.language.NodeTraverser;
import graphql.language.NodeVisitor;
import graphql.parser.Parser;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

public class JavaGenerator {

    private JavaConfiguration config;

    private JavaContext context;

    private Document document;

    private TypeDefinitionRegistry registry;

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

        NodeVisitor visitor = new JavaGeneratingNodeVisitor(context);
        NodeTraverser traverser = new NodeTraverser();
        traverser.preOrder(visitor, document);
    }

    private void loadSchema() throws IOException {
        String schemaText = new String(Files.readAllBytes(Paths.get(config.getSourceFile())), UTF_8);
        Parser parser = new Parser();
        document = parser.parseDocument(schemaText);
        SchemaParser schemaParser = new SchemaParser();
        registry = schemaParser.buildRegistry(document);
    }
}
