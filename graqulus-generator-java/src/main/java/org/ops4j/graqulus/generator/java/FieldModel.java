package org.ops4j.graqulus.generator.java;

import java.util.List;
import java.util.Objects;

import graphql.language.FieldDefinition;

public class FieldModel {

    private FieldDefinition fieldDefinition;
    private String fieldName;
    private String typeName;
    private boolean notNullRequired;
    private boolean listRequired;
    private boolean overrideRequired;
    private List<InputValueModel> inputValues;

    public FieldDefinition getFieldDefinition() {
        return fieldDefinition;
    }

    public void setFieldDefinition(FieldDefinition fieldDefinition) {
        this.fieldDefinition = fieldDefinition;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        Objects.requireNonNull(typeName);
        this.typeName = typeName;
    }

    public boolean isNotNullRequired() {
        return notNullRequired;
    }

    public void setNotNullRequired(boolean notNullRequired) {
        this.notNullRequired = notNullRequired;
    }

    public boolean isListRequired() {
        return listRequired;
    }

    public void setListRequired(boolean listRequired) {
        this.listRequired = listRequired;
    }

    public boolean isOverrideRequired() {
        return overrideRequired;
    }

    public void setOverrideRequired(boolean overrideRequired) {
        this.overrideRequired = overrideRequired;
    }

    public List<InputValueModel> getInputValues() {
        return inputValues;
    }

    public void setInputValues(List<InputValueModel> inputValues) {
        this.inputValues = inputValues;
    }

    public String getGetterName() {
        String prefix = typeName.equals("boolean") ? "is" : "get";
        return getAccessorName(prefix);
    }

    public String getSetterName() {
        return getAccessorName("set");
    }

    private String getAccessorName(String prefix) {
        int start = 0;
        if (fieldName.startsWith("$")) {
            start++;
        }
        StringBuilder buffer = new StringBuilder(prefix);
        buffer.append(fieldName.substring(start, start + 1).toUpperCase());
        buffer.append(fieldName.substring(start + 1));
        return buffer.toString();
    }
}
