package org.ops4j.graqulus.generator.java;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.ops4j.graqulus.generator.trimou.TemplateEngine;

import graphql.language.EnumTypeDefinition;
import graphql.language.EnumValueDefinition;
import graphql.language.FieldDefinition;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.ListType;
import graphql.language.Node;
import graphql.language.NodeVisitorStub;
import graphql.language.NonNullType;
import graphql.language.ObjectTypeDefinition;
import graphql.language.OperationTypeDefinition;
import graphql.language.ScalarTypeDefinition;
import graphql.language.Type;
import graphql.language.TypeName;
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

    public JavaGeneratingNodeVisitor(JavaContext context) {
        this.context = context;
        this.templateEngine = context.getTemplateEngine();
        this.registry = context.getRegistry();
    }

    @Override
    public TraversalControl visitOperationTypeDefinition(OperationTypeDefinition node, TraverserContext<Node> context) {
        String operationType = getJavaType(node.getType());
        rootOperationTypes.add(operationType);
        return TraversalControl.CONTINUE;
    }


    @Override
    public TraversalControl visitObjectTypeDefinition(ObjectTypeDefinition node, TraverserContext<Node> context) {
        if (rootOperationTypes.contains(node.getName())) {
            return TraversalControl.CONTINUE;
        }
        InterfaceModel interfaceModel = new InterfaceModel();
        interfaceModel.setInterfaceType(null);
        interfaceModel.setPackageName(this.context.getConfig().getBasePackage());
        interfaceModel.setTypeName(node.getName());
        interfaceModel.setFieldModels(node.getFieldDefinitions().stream().map(f -> toFieldModel(f, node)).collect(toList()));
        interfaceModel.setInterfaces(node.getImplements().stream().map(this::getJavaType).collect(toList()));

        String javaInterface = templateEngine.renderTemplate("object", interfaceModel);
        writeJavaFile(javaInterface, node.getName());

        return TraversalControl.CONTINUE;
    }

    private void writeJavaFile(String content, String typeName) {
        Path outputPath = this.context.getPackageDir().toPath().resolve(typeName + ".java");
        try {
            Files.writeString(outputPath, content);
        } catch (IOException exc) {
            throw Exceptional.throwAsUnchecked(exc);
        }
    }

    @Override
    public TraversalControl visitInterfaceTypeDefinition(InterfaceTypeDefinition node, TraverserContext<Node> context) {
        InterfaceModel interfaceModel = new InterfaceModel();
        interfaceModel.setInterfaceType(node);
        interfaceModel.setPackageName(this.context.getConfig().getBasePackage());
        interfaceModel.setTypeName(node.getName());
        interfaceModel.setFieldModels(node.getFieldDefinitions().stream().map(f -> toFieldModel(f, null)).collect(toList()));

        String javaInterface = templateEngine.renderTemplate("interface", interfaceModel);
        writeJavaFile(javaInterface, node.getName());
        return TraversalControl.CONTINUE;
    }

    @Override
    public TraversalControl visitEnumTypeDefinition(EnumTypeDefinition node, TraverserContext<Node> context) {

        EnumModel interfaceModel = new EnumModel();
        interfaceModel.setPackageName(this.context.getConfig().getBasePackage());
        interfaceModel.setTypeName(node.getName());
        interfaceModel.setValueNames(node.getEnumValueDefinitions().stream().map(EnumValueDefinition::getName).collect(toList()));

        String javaInterface = templateEngine.renderTemplate("enum", interfaceModel);
        writeJavaFile(javaInterface, node.getName());
        return TraversalControl.CONTINUE;
    }

    private FieldModel toFieldModel(FieldDefinition fieldDefinition, ObjectTypeDefinition object) {
        FieldModel fieldModel = new FieldModel();
        fieldModel.setFieldDefinition(fieldDefinition);
        fieldModel.setFieldName(fieldDefinition.getName());
        fieldModel.setTypeName(getJavaType(fieldDefinition.getType()));
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
        String interfaceName = getJavaType(interfaceType);
        InterfaceTypeDefinition interfaceDefinition = registry.getType(interfaceName, InterfaceTypeDefinition.class).get();
        return interfaceDefinition.getFieldDefinitions().stream().map(FieldDefinition::getName).anyMatch(fieldName::equals);
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
