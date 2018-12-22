package org.ops4j.graqulus.github;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonValue;

import io.earcam.unexceptional.Exceptional;

public class JsonFileHelper {

	public static boolean useFiles() {
		return true;
	}

    public static <T extends JsonValue> T writeToFile(String baseName, T json) {
        try {
			Files.write(Paths.get("target", baseName + ".json"), json.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException exc) {
		    throw Exceptional.throwAsUnchecked(exc);
		}
        return json;
    }

    @SuppressWarnings("unchecked")
	public static <T extends JsonValue> T readFromFile(String baseName) {
        InputStream is = JsonFileHelper.class.getResourceAsStream("/github/json/" + baseName + ".json");
        JsonReader reader = Json.createReader(is);
        return (T) reader.readValue();
    }
}
