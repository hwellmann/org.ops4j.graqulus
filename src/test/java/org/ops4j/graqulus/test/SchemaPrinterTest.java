package org.ops4j.graqulus.test;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ops4j.graqulus.generator.java.EnumModel;
import org.ops4j.graqulus.generator.java.FieldModel;
import org.ops4j.graqulus.generator.java.InterfaceModel;
import org.ops4j.graqulus.generator.trimou.TemplateEngine;

import graphql.language.Document;
import graphql.language.EnumTypeDefinition;
import graphql.language.EnumValueDefinition;
import graphql.language.FieldDefinition;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.ObjectTypeDefinition;
import graphql.language.ScalarTypeDefinition;
import graphql.language.Type;
import graphql.language.TypeName;
import graphql.parser.Parser;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.SchemaPrinter;
import graphql.schema.idl.TypeDefinitionRegistry;

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
        Scanner scanner = new Scanner(is, charset);
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

        InterfaceModel interfaceModel = new InterfaceModel();
        interfaceModel.setInterfaceType(character);
        interfaceModel.setPackageName("org.ops4j.japhql.starwars");
        interfaceModel.setTypeName(character.getName());
        interfaceModel.setFieldModels(character.getFieldDefinitions().stream().map(this::toFieldModel).collect(toList()));

        TemplateEngine templateEngine = new TemplateEngine();
        String javaInterface = templateEngine.renderTemplate("interface", interfaceModel);
        System.out.println(javaInterface);
    }

    @Test
    public void shouldExamineObject() {
        ObjectTypeDefinition character = registry.getType("Droid", ObjectTypeDefinition.class).get();

        InterfaceModel interfaceModel = new InterfaceModel();
        interfaceModel.setInterfaceType(null);
        interfaceModel.setPackageName("org.ops4j.japhql.starwars");
        interfaceModel.setTypeName(character.getName());
        interfaceModel.setFieldModels(character.getFieldDefinitions().stream().map(this::toFieldModel).collect(toList()));

        TemplateEngine templateEngine = new TemplateEngine();
        String javaInterface = templateEngine.renderTemplate("object", interfaceModel);
        System.out.println(javaInterface);
    }

    @Test
    public void shouldExamineEnumeration() {
        EnumTypeDefinition character = registry.getType("Episode", EnumTypeDefinition.class).get();

        EnumModel interfaceModel = new EnumModel();
        interfaceModel.setPackageName("org.ops4j.japhql.starwars");
        interfaceModel.setTypeName(character.getName());
        interfaceModel.setValueNames(character.getEnumValueDefinitions().stream().map(EnumValueDefinition::getName).collect(toList()));

        TemplateEngine templateEngine = new TemplateEngine();
        String javaInterface = templateEngine.renderTemplate("enum", interfaceModel);
        System.out.println(javaInterface);
    }

    private FieldModel toFieldModel(FieldDefinition fieldDefinition) {
        FieldModel fieldModel = new FieldModel();
        fieldModel.setFieldDefinition(fieldDefinition);
        fieldModel.setFieldName(fieldDefinition.getName());
        fieldModel.setTypeName(getJavaType(fieldDefinition.getType()));
        if (fieldModel.getTypeName().startsWith("List<")) {
            fieldModel.setListRequired(true);
        }
        return fieldModel;
    }


    private String getJavaType(Type<?> type) {
        if (type instanceof TypeName) {
            TypeName typeName = (TypeName) type;
            String javaName = typeName.getName();
            Optional<ScalarTypeDefinition> scalarType = registry.getType(javaName, ScalarTypeDefinition.class);
            if (scalarType.isPresent()) {
                javaName = mapScalarTypeName(scalarType.get());
            }

            return javaName;
        }
        if (type instanceof NonNullType) {
            NonNullType nonNullType = (NonNullType) type;
            return getJavaType(nonNullType.getType());
        }
        if (type instanceof ListType) {
            ListType listType = (ListType) type;
            return String.format("List<%s>", getJavaType(listType.getType()));
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
