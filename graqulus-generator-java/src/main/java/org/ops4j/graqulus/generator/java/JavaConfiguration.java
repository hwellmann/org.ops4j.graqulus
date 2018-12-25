package org.ops4j.graqulus.generator.java;

import java.util.List;

public class JavaConfiguration {

    private String basePackage;

    private List<String> sourceFiles;

    private String targetDir;

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public List<String> getSourceFiles() {
        return sourceFiles;
    }

    public void setSourceFiles(List<String> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    public String getTargetDir() {
        return targetDir;
    }

    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }
}
