package org.ops4j.graqulus.maven;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.ops4j.graqulus.generator.java.JavaConfiguration;
import org.ops4j.graqulus.generator.java.JavaGenerator;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Generates Java sources from a RAML model.
 *
 * @author Harald Wellmann
 *
 */
public abstract class AbstractJavaMojo extends AbstractMojo {

    /** RAML specification file. */
    @Parameter(required = true)
    protected String model;

    @Parameter(readonly = true, defaultValue = "${project}")
    protected MavenProject project;

    @Inject
    private BuildContext buildContext;

    /**
     * Fully qualified package name for generated Java sources. The generated classes will be located in subpackages
     * {@code model} and {@code api}.
     */
    @Parameter(name = "package", required = true)
    private String packageName;

    /**
     * @throws MojoFailureException
     */
    protected void generateJavaSources() throws MojoFailureException {
        if (buildContext.hasDelta(model)) {
            getLog().info("Generating Java model from " + model);
            JavaConfiguration config = new JavaConfiguration();
            config.setSourceFile(model);
            config.setBasePackage(packageName);
            config.setTargetDir(getOutputDir().toString());

            JavaGenerator generator = new JavaGenerator(config);
            try {
                generator.generate();
            } catch (IOException exc) {
                throw new MojoFailureException("code generation failed", exc);
            }

            refreshGeneratedSources();
        } else {
            getLog().info("Java model is up-to-date");
        }
    }

    private void refreshGeneratedSources() {
        getLog().debug("refreshing " + getOutputDir());
        buildContext.refresh(getOutputDir());
    }

    public abstract File getOutputDir();

    /**
     * Gets the package name for the generated sources.
     *
     * @return the package name
     */
    public String getPackage() {
        return packageName;
    }

    /**
     * Gets the package name for the generated sources.
     *
     * @param packageName the genPackage to set
     */
    // CHECKSTYLE:SKIP
    public void setPackage(String packageName) {
        this.packageName = packageName;
    }
}
