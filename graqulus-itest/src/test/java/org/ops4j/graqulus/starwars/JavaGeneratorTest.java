package org.ops4j.graqulus.starwars;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.ops4j.graqulus.generator.java.JavaConfiguration;
import org.ops4j.graqulus.generator.java.JavaGenerator;

public class JavaGeneratorTest {

    @Test
    public void shouldGenerateStarWars() throws IOException {
        JavaConfiguration config = new JavaConfiguration();
        config.setSourceFiles(Arrays.asList(
                "src/test/resources/starWars.graphqls",
                "src/test/resources/starWarsDirectives.graphqls",
                "src/test/resources/starWarsExtensions.graphqls"));
        config.setBasePackage("org.ops4j.graqulus.starwars");
        config.setTargetDir("target/graphql");

        JavaGenerator generator = new JavaGenerator(config);
        generator.generate();
    }
}
