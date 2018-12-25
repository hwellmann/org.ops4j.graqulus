package org.ops4j.graqulus.generator.java;

import java.io.File;

import org.ops4j.graqulus.generator.trimou.TemplateEngine;

import graphql.schema.GraphQLSchema;

public class JavaContext {

    private JavaConfiguration config;
    private File packageDir;
    private TemplateEngine templateEngine;
    private GraphQLSchema schema;

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

    public TemplateEngine getTemplateEngine() {
        return templateEngine;
    }

    public void setTemplateEngine(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public GraphQLSchema getSchema() {
        return schema;
    }

    public void setSchema(GraphQLSchema schema) {
        this.schema = schema;
    }
}
