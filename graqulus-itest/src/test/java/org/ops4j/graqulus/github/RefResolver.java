package org.ops4j.graqulus.github;

import static org.ops4j.graqulus.github.JsonFileHelper.useFiles;
import static org.ops4j.graqulus.github.JsonFileHelper.writeToFile;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.client.WebTarget;

import org.ops4j.graqulus.cdi.api.ResolveField;
import org.ops4j.graqulus.cdi.api.Resolver;

@Alternative
@ApplicationScoped
public class RefResolver implements Resolver<Ref> {

	@Inject
	private WebTarget target;

	@Override
	public List<String> loadById() {
		return Collections.singletonList("target");
	}

	@ResolveField
	public CompletionStage<Commit> target(Ref ref, String owner, String name) {
		String id = ref.getTarget().getId();
		return useFiles() ? targetFromFile(owner, name, id) : targetFromApi(owner, name, id);
	}

	private CompletionStage<Commit> targetFromApi(String owner, String name, String id) {
		return target.path("repos/{owner}/{name}/commits/{sha}")
		        .resolveTemplate("owner", owner)
				.resolveTemplate("name", name)
				.resolveTemplate("sha", id)
				.request().rx().get(JsonObject.class)
				.thenApply(j -> writeToFile(String.format("commit_%s_%s_%s", owner, name, id), j))
				.thenApply(this::toCommit);
	}

	private CompletionStage<Commit> targetFromFile(String owner, String name, String id) {
		return CompletableFuture.supplyAsync(
				() -> JsonFileHelper.<JsonObject>readFromFile(String.format("commit_%s_%s_%s", owner, name, id)))
				.thenApply(this::toCommit);
	}

	private Commit toCommit(JsonObject json) {
		Commit commit = new Commit();

		commit.setId(json.getString("sha"));
		commit.setOid(json.getString("sha"));
		commit.setMessage(json.getJsonObject("commit").getString("message"));
		commit.setCommittedDate(json.getJsonObject("commit").getJsonObject("committer").getString("date"));
		commit.setChangedFiles(json.getJsonArray("files").size());

		GitActor committer = new GitActor();
		User user = new User();
		user.setLogin(json.getJsonObject("committer").getString("login"));
		committer.setUser(user);
		commit.setCommitter(committer);

		return commit;
	}
}
