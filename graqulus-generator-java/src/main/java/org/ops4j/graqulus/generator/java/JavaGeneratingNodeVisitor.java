package org.ops4j.graqulus.generator.java;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.ops4j.graqulus.generator.trimou.TemplateEngine;
import org.ops4j.graqulus.shared.OperationTypeRegistry;

import graphql.language.EnumTypeDefinition;
import graphql.language.EnumValueDefinition;
import graphql.language.FieldDefinition;
import graphql.language.InputObjectTypeDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.Node;
import graphql.language.NodeVisitorStub;
import graphql.language.ObjectTypeDefinition;
import graphql.language.Type;
import graphql.language.UnionTypeDefinition;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeInfo;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import io.earcam.unexceptional.Exceptional;

@SuppressWarnings("rawtypes")
public class JavaGeneratingNodeVisitor extends NodeVisitorStub {

    private TypeDefinitionRegistry registry;
    private TemplateEngine templateEngine;
    private JavaContext context;
    private JavaTypeMapper typeMapper;
    private OperationTypeRegistry operationTypeRegistry;

    public JavaGeneratingNodeVisitor(JavaContext javaContext) {
        this.context = javaContext;
        this.templateEngine = javaContext.getTemplateEngine();
        this.registry = javaContext.getRegistry();
        this.operationTypeRegistry = new OperationTypeRegistry(registry);
        this.typeMapper = new JavaTypeMapper(registry);
    }

    @Override
    public TraversalControl visitObjectTypeDefinition(ObjectTypeDefinition node, TraverserContext<Node> ctx) {
        if (operationTypeRegistry.isOperationType(node)) {
            generateRootOperation(node);
            return TraversalControl.CONTINUE;
        }
        CompositeModel model = new CompositeModel();
        model.setInterfaceType(null);
        model.setPackageName(this.context.getConfig().getBasePackage());
        model.setTypeName(node.getName());
        model.setFieldModels(node.getFieldDefinitions().stream()
                .map(f -> toFieldModel(f, node)).collect(toList()));
        model.setInterfaces(node.getImplements().stream().map(typeMapper::toJavaType).collect(toList()));

        String javaInterface = templateEngine.renderTemplate("object", model);
        writeJavaFile(javaInterface, node.getName());

        return TraversalControl.CONTINUE;
    }

    private void generateRootOperation(ObjectTypeDefinition node) {
        CompositeModel model = new CompositeModel();
        model.setInterfaceType(null);
        model.setPackageName(this.context.getConfig().getBasePackage());
        model.setTypeName(node.getName());
        model.setFieldModels(node.getFieldDefinitions().stream()
                .map(f -> toFieldModel(f, node)).collect(toList()));
        model.setInterfaces(node.getImplements().stream().map(typeMapper::toJavaType).collect(toList()));

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
    public TraversalControl visitInterfaceTypeDefinition(InterfaceTypeDefinition node, TraverserContext<Node> ctx) {
        CompositeModel model = new CompositeModel();
        model.setInterfaceType(node);
        model.setPackageName(this.context.getConfig().getBasePackage());
        model.setTypeName(node.getName());
        model.setFieldModels(node.getFieldDefinitions().stream().map(f -> toFieldModel(f, null)).collect(toList()));

        String javaInterface = templateEngine.renderTemplate("interface", model);
        writeJavaFile(javaInterface, node.getName());
        return TraversalControl.CONTINUE;
    }

    @Override
    public TraversalControl visitInputObjectTypeDefinition(InputObjectTypeDefinition node,
            TraverserContext<Node> ctx) {
        CompositeModel model = new CompositeModel();
        model.setInterfaceType(null);
        model.setPackageName(this.context.getConfig().getBasePackage());
        model.setTypeName(node.getName());
        model.setFieldModels(node.getInputValueDefinitions().stream().map(f -> toFieldModel(f)).collect(toList()));

        String javaInterface = templateEngine.renderTemplate("inputObject", model);
        writeJavaFile(javaInterface, node.getName());

        return TraversalControl.CONTINUE;
    }

    @Override
    public TraversalControl visitUnionTypeDefinition(UnionTypeDefinition node, TraverserContext<Node> ctx) {
        CompositeModel model = new CompositeModel();
        model.setInterfaceType(null);
        model.setPackageName(this.context.getConfig().getBasePackage());
        model.setTypeName(node.getName());
        model.setFieldModels(node.getMemberTypes().stream().map(m -> toFieldModel(m)).collect(toList()));

        String javaInterface = templateEngine.renderTemplate("union", model);
        writeJavaFile(javaInterface, node.getName());

        return TraversalControl.CONTINUE;
    }

    @Override
    public TraversalControl visitEnumTypeDefinition(EnumTypeDefinition node, TraverserContext<Node> ctx) {

        EnumModel model = new EnumModel();
        model.setPackageName(this.context.getConfig().getBasePackage());
        model.setTypeName(node.getName());
        model.setValueNames(
                node.getEnumValueDefinitions().stream().map(EnumValueDefinition::getName).collect(toList()));

        String javaInterface = templateEngine.renderTemplate("enum", model);
        writeJavaFile(javaInterface, node.getName());
        return TraversalControl.CONTINUE;
    }

    private FieldModel toFieldModel(FieldDefinition fieldDefinition, ObjectTypeDefinition object) {
        FieldModel fieldModel = new FieldModel();
        fieldModel.setFieldDefinition(fieldDefinition);
        fieldModel.setFieldName(typeMapper.toJavaVariable(fieldDefinition.getName()));
        fieldModel.setTypeName(typeMapper.toJavaType(fieldDefinition.getType()));
        if (TypeInfo.typeInfo(fieldDefinition.getType()).isList()) {
            fieldModel.setListRequired(true);
        }
        fieldModel.setOverrideRequired(isOverride(fieldDefinition, object));
        fieldModel.setInputValues(fieldDefinition.getInputValueDefinitions().stream()
                .map(this::toInputValueModel).collect(toList()));
        return fieldModel;
    }

    private FieldModel toFieldModel(Type type) {
        String typeName = typeMapper.toJavaType(type);
        String fieldName = typeName.substring(0, 1).toLowerCase() + typeName.substring(1);
        FieldModel fieldModel = new FieldModel();
        fieldModel.setFieldName(fieldName);
        fieldModel.setTypeName(typeName);
        return fieldModel;
    }

    private InputValueModel toInputValueModel(InputValueDefinition inputValueDefinition) {
        InputValueModel inputValueModel = new InputValueModel();
        inputValueModel.setFieldName(typeMapper.toJavaVariable(inputValueDefinition.getName()));
        inputValueModel.setTypeName(typeMapper.toJavaType(inputValueDefinition.getType()));
        return inputValueModel;
    }

    private FieldModel toFieldModel(InputValueDefinition inputValueDefinition) {
        FieldModel fieldModel = new FieldModel();
        fieldModel.setFieldName(typeMapper.toJavaVariable(inputValueDefinition.getName()));
        fieldModel.setTypeName(typeMapper.toJavaType(inputValueDefinition.getType()));
        if (TypeInfo.typeInfo(inputValueDefinition.getType()).isList()) {
            fieldModel.setListRequired(true);
        }
        return fieldModel;
    }

    private boolean isOverride(FieldDefinition fieldDefinition, ObjectTypeDefinition object) {
        if (object == null) {
            return false;
        }
        String fieldName = fieldDefinition.getName();
        for (Type interfaceType : object.getImplements()) {
            if (hasField(interfaceType, fieldName)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasField(Type interfaceType, String fieldName) {
        String interfaceName = typeMapper.toJavaType(interfaceType);
        InterfaceTypeDefinition interfaceDefinition = registry.getType(interfaceName, InterfaceTypeDefinition.class)
                .get();
        return interfaceDefinition.getFieldDefinitions().stream().map(FieldDefinition::getName)
                .anyMatch(fieldName::equals);
    }
}
