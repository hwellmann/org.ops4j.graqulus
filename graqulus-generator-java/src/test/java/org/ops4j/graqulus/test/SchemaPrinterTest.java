package org.ops4j.graqulus.test;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.awt.List;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ops4j.graqulus.generator.java.CompositeModel;
import org.ops4j.graqulus.generator.java.EnumModel;
import org.ops4j.graqulus.generator.java.EnumValueModel;
import org.ops4j.graqulus.generator.java.FieldModel;
import org.ops4j.graqulus.generator.java.JavaType;
import org.ops4j.graqulus.generator.trimou.TemplateEngine;

import graphql.language.Document;
import graphql.language.EnumTypeDefinition;
import graphql.language.FieldDefinition;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.ObjectTypeDefinition;
import graphql.language.ScalarTypeDefinition;
import graphql.language.Type;
import graphql.language.TypeName;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.SchemaPrinter;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.UnExecutableSchemaGenerator;

public class SchemaPrinterTest {

    private Document document;
    private TypeDefinitionRegistry registry;

    @BeforeEach
    public void before() throws IOException {
        loadSchema();
    }

    private void loadSchema() throws IOException {
        InputStream is = getClass().getResourceAsStream("/starWarsSchemaAnnotated.graphqls");
        Parser parser = new Parser();
        String schemaText = readWholeInputStream(is, StandardCharsets.UTF_8);
        document = parser.parseDocument(schemaText);
        SchemaParser schemaParser = new SchemaParser();
        registry = schemaParser.buildRegistry(document);
    }

    private String readWholeInputStream(InputStream is, Charset charset) {
        Scanner scanner = new Scanner(is, charset.name());
        scanner.useDelimiter("\\Z");
        String text = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        return text;
    }

    @Test
    public void shouldPrintSchema() {
        SchemaPrinter schemaPrinter = new SchemaPrinter();
        System.out.println(schemaPrinter.print(document));
    }

    @Test
    public void shouldExamineInterface() {
        InterfaceTypeDefinition character = registry.getType("Character", InterfaceTypeDefinition.class).get();

        CompositeModel interfaceModel = new CompositeModel();
        interfaceModel.setInterfaceType(character);
        interfaceModel.setPackageName("org.ops4j.graqulus.starwars");
        interfaceModel.setTypeName(character.getName());
        interfaceModel
                .setFieldModels(character.getFieldDefinitions().stream().map(this::toFieldModel).collect(toList()));

        TemplateEngine templateEngine = new TemplateEngine();
        String javaInterface = templateEngine.renderTemplate("interface", interfaceModel);
        System.out.println(javaInterface);
    }

    @Test
    public void shouldExamineObject() {
        ObjectTypeDefinition character = registry.getType("Droid", ObjectTypeDefinition.class).get();

        CompositeModel interfaceModel = new CompositeModel();
        interfaceModel.setInterfaceType(null);
        interfaceModel.setPackageName("org.ops4j.graqulus.starwars");
        interfaceModel.setTypeName(character.getName());
        interfaceModel
                .setFieldModels(character.getFieldDefinitions().stream().map(this::toFieldModel).collect(toList()));

        TemplateEngine templateEngine = new TemplateEngine();
        String javaInterface = templateEngine.renderTemplate("object", interfaceModel);
        System.out.println(javaInterface);
    }

    @Test
    public void shouldExamineEnumeration() {
        EnumTypeDefinition character = registry.getType("Episode", EnumTypeDefinition.class).get();

        EnumModel interfaceModel = new EnumModel();
        interfaceModel.setPackageName("org.ops4j.graqulus.starwars");
        interfaceModel.setTypeName(character.getName());
        interfaceModel.setValueModels(character.getEnumValueDefinitions().stream()
                .map(e -> new EnumValueModel(e.getName(), null))
                .collect(toList()));

        TemplateEngine templateEngine = new TemplateEngine();
        String javaInterface = templateEngine.renderTemplate("enum", interfaceModel);
        System.out.println(javaInterface);
    }

    @Test
    public void shouldMakeUnexecutableSchema() {
        GraphQLSchema schema = UnExecutableSchemaGenerator.makeUnExecutableSchema(registry);
        assertThat(schema).isNotNull();
    }

    private FieldModel toFieldModel(FieldDefinition fieldDefinition) {
        FieldModel fieldModel = new FieldModel();
        fieldModel.setFieldDefinition(fieldDefinition);
        fieldModel.setFieldName(fieldDefinition.getName());
        fieldModel.setType(getJavaType(fieldDefinition.getType()));
        return fieldModel;
    }

    private JavaType getJavaType(Type<?> type) {
        if (type instanceof TypeName) {
            TypeName typeName = (TypeName) type;
            String javaName = typeName.getName();
            Optional<ScalarTypeDefinition> scalarType = registry.getType(javaName, ScalarTypeDefinition.class);
            if (scalarType.isPresent()) {
                javaName = mapScalarTypeName(scalarType.get());
            }

            return new JavaType(javaName);
        }
        if (type instanceof NonNullType) {
            NonNullType nonNullType = (NonNullType) type;
            return getJavaType(nonNullType.getType());
        }
        if (type instanceof ListType) {
            ListType listType = (ListType) type;
            return new JavaType(String.format("List<%s>", getJavaType(listType.getType())), List.class.getName());
        }
        throw new IllegalArgumentException("unknown type");
    }

    private String mapScalarTypeName(ScalarTypeDefinition scalarType) {
        String gqlName = scalarType.getName();
        if (gqlName.equals("ID")) {
            return "String";
        }
        return gqlName;
    }
}
