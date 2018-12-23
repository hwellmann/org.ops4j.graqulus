package org.ops4j.graqulus.github;

import static java.util.stream.Collectors.toList;
import static org.ops4j.graqulus.github.JsonFileHelper.readFromFile;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;

import org.ops4j.graqulus.cdi.api.BatchLoader;
import org.ops4j.graqulus.cdi.api.Resolver;

@ApplicationScoped
public class BatchRefResolver implements Resolver<Ref> {

	@Override
	public List<String> loadById() {
		return Collections.singletonList("target");
	}

	@BatchLoader
	public List<GitObject> load(List<String> ids, Function<String, String> owner, Function<String, String> name) {
		return ids.stream()
				.map(id -> toCommit(owner.apply(id), name.apply(id), id))
				.collect(toList());
	}

	private Commit toCommit(String owner, String name, String id) {
		JsonObject json = readFromFile(String.format("commit_%s_%s_%s", owner, name, id));
		return toCommit(json);
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
