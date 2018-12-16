package org.ops4j.graqulus.maven;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Generates Java sources from a RAML model.
 *
 * @author Harald Wellmann
 *
 */
@Mojo(name = "java", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class JavaMojo extends AbstractJavaMojo {

    /**
     * Output directory for generated sources.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/graqulus")
    private File outputDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String outputRoot = getOutputDir().getAbsolutePath();
        getLog().info("Generating additional source directory " + outputRoot);
        project.addCompileSourceRoot(outputRoot);
        generateJavaSources();
    }

    @Override
    public File getOutputDir() {
        return outputDir;
    }
}
