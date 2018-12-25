package org.ops4j.graqulus.generator.java;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Generated;

import graphql.language.InterfaceTypeDefinition;

public class CompositeModel {

    private String packageName;

    private InterfaceTypeDefinition interfaceType;

    private String typeName;

    private List<FieldModel> fieldModels;

    private String date;

    private List<String> interfaces;

    private String description;

    public CompositeModel() {
        this.date = ZonedDateTime.now().truncatedTo(SECONDS).format(ISO_OFFSET_DATE_TIME);
    }

    public InterfaceTypeDefinition getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(InterfaceTypeDefinition interfaceType) {
        this.interfaceType = interfaceType;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public List<FieldModel> getFieldModels() {
        return fieldModels;
    }

    public void setFieldModels(List<FieldModel> fieldModels) {
        this.fieldModels = fieldModels;
    }

    public boolean isNonNullRequired() {
        return fieldModels.stream().anyMatch(FieldModel::isNotNullRequired);
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<String> interfaces) {
        this.interfaces = interfaces;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getImports() {
        Set<String> imports = new TreeSet<>();
        imports.add(Generated.class.getName());
        fieldModels.stream()
                .map(FieldModel::getType)
                .map(JavaType::getImportName)
                .filter(Objects::nonNull)
                .forEach(imports::add);
        return imports;
    }
}
