package org.ops4j.graqulus.generator.java;

public class InputValueModel {

    private String fieldName;
    private JavaType type;

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
}
