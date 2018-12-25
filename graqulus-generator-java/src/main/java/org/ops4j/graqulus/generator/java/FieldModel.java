package org.ops4j.graqulus.generator.java;

import java.util.List;

public class FieldModel {

    private String description;
    private String fieldName;
    private JavaType type;
    private boolean notNullRequired;
    private boolean overrideRequired;
    private List<InputValueModel> inputValues;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public JavaType getType() {
        return type;
    }

    public void setType(JavaType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isNotNullRequired() {
        return notNullRequired;
    }

    public void setNotNullRequired(boolean notNullRequired) {
        this.notNullRequired = notNullRequired;
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
        String prefix = type.getName().equals("boolean") ? "is" : "get";
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
