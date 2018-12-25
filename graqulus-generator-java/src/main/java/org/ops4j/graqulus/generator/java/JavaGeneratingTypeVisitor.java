package org.ops4j.graqulus.generator.java;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.ops4j.graqulus.generator.trimou.TemplateEngine;
import org.ops4j.graqulus.shared.OperationTypeRegistry;

import graphql.language.FieldDefinition;
import graphql.language.InterfaceTypeDefinition;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.schema.GraphQLUnionType;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import io.earcam.unexceptional.Exceptional;

public class JavaGeneratingTypeVisitor extends GraphQLTypeVisitorStub {

    private TypeDefinitionRegistry registry;
    private TemplateEngine templateEngine;
    private JavaContext context;
    private JavaTypeMapper typeMapper;
    private OperationTypeRegistry operationTypeRegistry;

    public JavaGeneratingTypeVisitor(JavaContext javaContext) {
        this.context = javaContext;
        this.templateEngine = javaContext.getTemplateEngine();
        this.registry = javaContext.getRegistry();
        this.operationTypeRegistry = new OperationTypeRegistry(registry);
        this.typeMapper = new JavaTypeMapper();
    }

    @Override
    public TraversalControl visitGraphQLObjectType(GraphQLObjectType node, TraverserContext<GraphQLType> ctx) {
        if (node.getDefinition() == null) {
            return TraversalControl.CONTINUE;
        }
        if (operationTypeRegistry.isOperationType(node.getName())) {
            generateRootOperation(node);
            return TraversalControl.CONTINUE;
        }
        CompositeModel model = new CompositeModel();
        model.setDescription(node.getDescription());
        model.setInterfaceType(null);
        model.setPackageName(this.context.getConfig().getBasePackage());
        model.setTypeName(node.getName());
        model.setFieldModels(node.getFieldDefinitions().stream()
                .map(f -> toFieldModel(f, node)).collect(toList()));
        model.setInterfaces(node.getInterfaces().stream()
                .map(typeMapper::toJavaType).map(JavaType::getName).collect(toList()));

        String javaInterface = templateEngine.renderTemplate("object", model);
        writeJavaFile(javaInterface, node.getName());

        return TraversalControl.CONTINUE;
    }

    private void generateRootOperation(GraphQLObjectType node) {
        CompositeModel model = new CompositeModel();
        model.setDescription(node.getDescription());
        model.setInterfaceType(null);
        model.setPackageName(this.context.getConfig().getBasePackage());
        model.setTypeName(node.getName());
        model.setFieldModels(node.getFieldDefinitions().stream()
                .map(f -> toFieldModel(f, node)).collect(toList()));
        model.setInterfaces(node.getInterfaces().stream()
                .map(typeMapper::toJavaType).map(JavaType::getName).collect(toList()));

        String javaInterface = templateEngine.renderTemplate("rootOperation", model);
        writeJavaFile(javaInterface, node.getName());
    }

    private void writeJavaFile(String content, String typeName) {
        Path outputPath = this.context.getPackageDir().toPath().resolve(typeName + ".java");
        try {
            Files.write(outputPath, content.getBytes(UTF_8));
        } catch (IOException exc) {
            throw Exceptional.throwAsUnchecked(exc);
        }
    }

    @Override
    public TraversalControl visitGraphQLInterfaceType(GraphQLInterfaceType node, TraverserContext<GraphQLType> ctx) {
        CompositeModel model = new CompositeModel();
        model.setDescription(node.getDescription());
        model.setInterfaceType(node.getDefinition());
        model.setPackageName(this.context.getConfig().getBasePackage());
        model.setTypeName(node.getName());
        model.setFieldModels(node.getFieldDefinitions().stream().map(f -> toFieldModel(f, null)).collect(toList()));

        String javaInterface = templateEngine.renderTemplate("interface", model);
        writeJavaFile(javaInterface, node.getName());
        return TraversalControl.CONTINUE;
    }

    @Override
    public TraversalControl visitGraphQLInputObjectType(GraphQLInputObjectType node,
            TraverserContext<GraphQLType> ctx) {
        CompositeModel model = new CompositeModel();
        model.setDescription(node.getDescription());
        model.setInterfaceType(null);
        model.setPackageName(this.context.getConfig().getBasePackage());
        model.setTypeName(node.getName());
        model.setFieldModels(node.getFieldDefinitions().stream()
                .map(f -> toFieldModel(f))
                .collect(toList()));

        String javaInterface = templateEngine.renderTemplate("inputObject", model);
        writeJavaFile(javaInterface, node.getName());

        return TraversalControl.CONTINUE;
    }

    @Override
    public TraversalControl visitGraphQLUnionType(GraphQLUnionType node, TraverserContext<GraphQLType> ctx) {
        CompositeModel model = new CompositeModel();
        model.setDescription(node.getDescription());
        model.setInterfaceType(null);
        model.setPackageName(this.context.getConfig().getBasePackage());
        model.setTypeName(node.getName());
        model.setFieldModels(
                node.getTypes().stream().map(m -> toFieldModel(m)).collect(toList()));

        String javaInterface = templateEngine.renderTemplate("union", model);
        writeJavaFile(javaInterface, node.getName());

        return TraversalControl.CONTINUE;
    }

    @Override
    public TraversalControl visitGraphQLEnumType(GraphQLEnumType node, TraverserContext<GraphQLType> ctx) {
        if (node.getDefinition() == null) {
            return TraversalControl.CONTINUE;
        }

        EnumModel model = new EnumModel();
        model.setDescription(node.getDescription());
        model.setPackageName(this.context.getConfig().getBasePackage());
        model.setTypeName(node.getName());
        model.setValueModels(node.getValues().stream()
                .map(this::toEnumValueModel)
                .collect(toList()));

        String javaInterface = templateEngine.renderTemplate("enum", model);
        writeJavaFile(javaInterface, node.getName());
        return TraversalControl.CONTINUE;
    }

    private EnumValueModel toEnumValueModel(GraphQLEnumValueDefinition enumValue) {
        return new EnumValueModel(enumValue.getName(), enumValue.getDescription());
    }

    private FieldModel toFieldModel(GraphQLFieldDefinition fieldDefinition, GraphQLObjectType object) {
        FieldDefinition definition = fieldDefinition.getDefinition();
        FieldModel fieldModel = new FieldModel();
        fieldModel.setDescription(fieldDefinition.getDescription());
        fieldModel.setFieldDefinition(definition);
        fieldModel.setFieldName(typeMapper.toJavaVariable(fieldDefinition.getName()));
        fieldModel.setType(typeMapper.toJavaType(fieldDefinition.getType()));
        fieldModel.setOverrideRequired(isOverride(definition, object));
        fieldModel.setInputValues(fieldDefinition.getArguments().stream()
                .map(this::toInputValueModel).collect(toList()));
        return fieldModel;
    }

    private FieldModel toFieldModel(GraphQLInputObjectField field) {
        FieldModel fieldModel = new FieldModel();
        fieldModel.setDescription(field.getDescription());
        fieldModel.setFieldName(typeMapper.toJavaVariable(field.getName()));
        fieldModel.setType(typeMapper.toJavaType(field.getType()));
        return fieldModel;
    }

    private FieldModel toFieldModel(GraphQLType type) {
        JavaType javaType = typeMapper.toJavaType(type);
        String typeName = javaType.getName();
        String fieldName = typeName.substring(0, 1).toLowerCase() + typeName.substring(1);
        FieldModel fieldModel = new FieldModel();
        fieldModel.setFieldName(typeMapper.toJavaVariable(fieldName));
        fieldModel.setType(javaType);
        return fieldModel;
    }

    private InputValueModel toInputValueModel(GraphQLArgument argument) {
        InputValueModel inputValueModel = new InputValueModel();
        inputValueModel.setFieldName(typeMapper.toJavaVariable(argument.getName()));
        inputValueModel.setType(typeMapper.toJavaType(argument.getType()));
        return inputValueModel;
    }

    private boolean isOverride(FieldDefinition fieldDefinition, GraphQLObjectType object) {
        if (object == null) {
            return false;
        }
        String fieldName = fieldDefinition.getName();
        for (GraphQLOutputType interfaceType : object.getInterfaces()) {
            if (hasField(interfaceType, fieldName)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasField(GraphQLOutputType interfaceType, String fieldName) {
        InterfaceTypeDefinition interfaceDefinition = registry
                .getType(interfaceType.getName(), InterfaceTypeDefinition.class)
                .get();
        return interfaceDefinition.getFieldDefinitions().stream().map(FieldDefinition::getName)
                .anyMatch(fieldName::equals);
    }
}
