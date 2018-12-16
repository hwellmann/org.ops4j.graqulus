package org.ops4j.graqulus.generator.util;

import java.io.File;
import java.io.IOException;

/**
 * Utility class for file system operations.
 *
 * @author Harald Wellmann
 *
 */
public class FileHelper {

    private FileHelper() {
        // hidden utility class constructor
    }

    /**
     * Checks is the given directory exists and creates it otherwise.
     * @param dir
     * @throws IOException
     */
    public static void createDirectoryIfNeeded(File dir) throws IOException {
        boolean success;
        if (dir.exists()) {
            success = dir.isDirectory();
        }
        else {
            success = dir.mkdirs();
        }
        if (!success) {
            throw new IOException("could not create " + dir);
        }
    }
}
