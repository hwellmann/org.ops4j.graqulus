package org.ops4j.graqulus.generator.java;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.ops4j.graqulus.generator.trimou.TemplateEngine;

import graphql.language.EnumTypeDefinition;
import graphql.language.EnumValueDefinition;
import graphql.language.FieldDefinition;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.Node;
import graphql.language.NodeVisitorStub;
import graphql.language.ObjectTypeDefinition;
import graphql.language.OperationTypeDefinition;
import graphql.language.Type;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import io.earcam.unexceptional.Exceptional;

@SuppressWarnings("rawtypes")
public class JavaGeneratingNodeVisitor extends NodeVisitorStub {

    private TypeDefinitionRegistry registry;
    private Set<String> rootOperationTypes = new HashSet<>();
    private TemplateEngine templateEngine;
    private JavaContext context;
    private JavaTypeMapper typeMapper;

    public JavaGeneratingNodeVisitor(JavaContext javaContext) {
        this.context = javaContext;
        this.templateEngine = javaContext.getTemplateEngine();
        this.registry = javaContext.getRegistry();
        this.typeMapper = new JavaTypeMapper(registry);
    }

    @Override
    public TraversalControl visitOperationTypeDefinition(OperationTypeDefinition node, TraverserContext<Node> ctx) {
        String operationType = typeMapper.toJavaType(node.getType());
        rootOperationTypes.add(operationType);
        return TraversalControl.CONTINUE;
    }

    @Override
    public TraversalControl visitObjectTypeDefinition(ObjectTypeDefinition node, TraverserContext<Node> ctx) {
        if (rootOperationTypes.contains(node.getName())) {
            return TraversalControl.CONTINUE;
        }
        InterfaceModel interfaceModel = new InterfaceModel();
        interfaceModel.setInterfaceType(null);
        interfaceModel.setPackageName(this.context.getConfig().getBasePackage());
        interfaceModel.setTypeName(node.getName());
        interfaceModel
                .setFieldModels(node.getFieldDefinitions().stream().map(f -> toFieldModel(f, node)).collect(toList()));
        interfaceModel.setInterfaces(node.getImplements().stream().map(typeMapper::toJavaType).collect(toList()));

        String javaInterface = templateEngine.renderTemplate("object", interfaceModel);
        writeJavaFile(javaInterface, node.getName());

        return TraversalControl.CONTINUE;
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
        InterfaceModel interfaceModel = new InterfaceModel();
        interfaceModel.setInterfaceType(node);
        interfaceModel.setPackageName(this.context.getConfig().getBasePackage());
        interfaceModel.setTypeName(node.getName());
        interfaceModel
                .setFieldModels(node.getFieldDefinitions().stream().map(f -> toFieldModel(f, null)).collect(toList()));

        String javaInterface = templateEngine.renderTemplate("interface", interfaceModel);
        writeJavaFile(javaInterface, node.getName());
        return TraversalControl.CONTINUE;
    }

    @Override
    public TraversalControl visitEnumTypeDefinition(EnumTypeDefinition node, TraverserContext<Node> ctx) {

        EnumModel interfaceModel = new EnumModel();
        interfaceModel.setPackageName(this.context.getConfig().getBasePackage());
        interfaceModel.setTypeName(node.getName());
        interfaceModel.setValueNames(
                node.getEnumValueDefinitions().stream().map(EnumValueDefinition::getName).collect(toList()));

        String javaInterface = templateEngine.renderTemplate("enum", interfaceModel);
        writeJavaFile(javaInterface, node.getName());
        return TraversalControl.CONTINUE;
    }

    private FieldModel toFieldModel(FieldDefinition fieldDefinition, ObjectTypeDefinition object) {
        FieldModel fieldModel = new FieldModel();
        fieldModel.setFieldDefinition(fieldDefinition);
        fieldModel.setFieldName(fieldDefinition.getName());
        fieldModel.setTypeName(typeMapper.toJavaType(fieldDefinition.getType()));
        if (fieldModel.getTypeName().startsWith("List<")) {
            fieldModel.setListRequired(true);
        }
        fieldModel.setOverrideRequired(isOverride(fieldDefinition, object));
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
