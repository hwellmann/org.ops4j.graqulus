package org.ops4j.graqulus.test;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.ops4j.graqulus.generator.java.JavaConfiguration;
import org.ops4j.graqulus.generator.java.JavaGenerator;

public class JavaGeneratorTest {

    @Test
    public void shouldGenerateStarWars() throws IOException {
        JavaConfiguration config = new JavaConfiguration();
        config.setSourceFile("src/test/resources/starWarsSchemaAnnotated.graphqls");
        config.setBasePackage("org.ops4j.graqulus.starwars");
        config.setTargetDir("target/graphql");

        JavaGenerator generator = new JavaGenerator(config);
        generator.generate();
    }
}
