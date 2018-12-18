package org.ops4j.graqulus.generator.java;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Constants {

    public static final Set<String> JAVA_KEYWORDS = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("abstract", "assert", "boolean", "break",
                    "byte", "case", "catch", "char", "class", "const", "continue", "default", "do",
                    "double", "else", "enum", "extends", "false", "final", "finally", "float", "for",
                    "goto", "if", "implements", "import", "instanceof", "int", "interface", "long",
                    "native", "new", "null", "package", "private", "protected", "public", "return", "short",
                    "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
                    "transient", "true", "try", "void", "volatile", "while")));

    private Constants() {
        throw new UnsupportedOperationException();
    }
}
