package org.ops4j.graqulus.generator.java;

import java.io.File;

import org.ops4j.graqulus.generator.trimou.TemplateEngine;

import graphql.schema.idl.TypeDefinitionRegistry;

public class JavaContext {

    private JavaConfiguration config;
    private File packageDir;
    private TypeDefinitionRegistry registry;
    private TemplateEngine templateEngine;

    public JavaContext(JavaConfiguration config) {
        this.config = config;
    }

    public JavaConfiguration getConfig() {
        return config;
    }

    public File getPackageDir() {
        return packageDir;
    }

    public void setPackageDir(File packageDir) {
        this.packageDir = packageDir;
    }

    public TypeDefinitionRegistry getRegistry() {
        return registry;
    }

    public void setRegistry(TypeDefinitionRegistry registry) {
        this.registry = registry;
    }

    public TemplateEngine getTemplateEngine() {
        return templateEngine;
    }

    public void setTemplateEngine(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }
}
