package org.ops4j.graqulus.github;

import static org.assertj.core.api.Assertions.assertThat;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class JsonFileHelperTest {

	@Test
	public void shouldReadJsonFile() {
		JsonObject json = JsonFileHelper.readFromFile("repo_wildfly_wildfly-core");
		assertThat(json.toString()).startsWith("{\"id\":21394954,\"node_id\":\"MDEwOlJlcG9zaXRvcnkyMTM5NDk1NA==\",\"name\":\"wildfly-core\",");
	}
}
