package org.ops4j.graqulus.generator.java;

public class JavaType {
    private String name;
    private String importName;

    public JavaType(String name) {
        this.name = name;
    }

    public JavaType(String name, String importName) {
        this.name = name;
        this.importName = importName;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getImportName() {
        return importName;
    }
    public void setImportName(String importName) {
        this.importName = importName;
    }
}
