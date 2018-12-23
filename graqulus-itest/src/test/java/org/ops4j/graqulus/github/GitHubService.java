package org.ops4j.graqulus.github;

import static org.ops4j.graqulus.github.JsonFileHelper.readFromFile;
import static org.ops4j.graqulus.github.JsonFileHelper.writeToFile;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.client.WebTarget;

import org.ops4j.graqulus.cdi.api.RootOperation;
import org.ops4j.graqulus.cdi.api.Schema;

@ApplicationScoped
@Schema(path = "githubPartial.graphqls", modelPackage = "org.ops4j.graqulus.cdi.github")
@RootOperation
public class GitHubService implements Query {

	@Inject
	private WebTarget target;

	@Override
	public Node node(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Node> nodes(List<String> ids) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Repository repository(String name, String owner) {
		JsonObject json = JsonFileHelper.useFiles() ? repoFromFile(name, owner) : repoFromApi(name, owner);
		if (json == null) {
			return null;
		}
		Repository repo = new Repository();
		repo.setCreatedAt(json.getString("created_at"));
		repo.setName(name);
		repo.setUrl(json.getString("clone_url"));
		return repo;
	}

	private JsonObject repoFromApi(String name, String owner) {
        JsonObject json = target.path("repos/{owner}/{name}")
                .resolveTemplate("owner", owner)
                .resolveTemplate("name", name)
                .request().get().readEntity(JsonObject.class);
        writeToFile(String.format("repo_%s_%s", owner, name), json);
        return json;
	}

	private JsonObject repoFromFile(String name, String owner) {
        return readFromFile(String.format("repo_%s_%s", owner, name));
    }
}
