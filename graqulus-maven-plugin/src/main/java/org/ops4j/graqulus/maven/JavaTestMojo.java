package org.ops4j.graqulus.maven;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Generates test sources from a RAML model.
 *
 * @author Harald Wellmann
 *
 */
@Mojo(name = "java-test", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES)
public class JavaTestMojo extends AbstractJavaMojo {

    /**
     * Output directory for generated sources.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-test-sources/graqulus")
    private File outputDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String outputRoot = getOutputDir().getAbsolutePath();
        getLog().info("Generating additional test source directory " + outputRoot);
        project.addTestCompileSourceRoot(outputRoot);
        generateJavaSources();
    }

    @Override
    public File getOutputDir() {
        return outputDir;
    }
}
