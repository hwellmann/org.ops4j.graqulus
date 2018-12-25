package org.ops4j.graqulus.generator.java;

public class EnumValueModel {
    private String name;
    private String description;

    public EnumValueModel() {
    }

    public EnumValueModel(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
