package org.ops4j.graqulus.github;

import static java.util.stream.Collectors.toList;
import static org.ops4j.graqulus.github.JsonFileHelper.writeToFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.client.WebTarget;

import org.ops4j.graqulus.cdi.api.ResolveField;
import org.ops4j.graqulus.cdi.api.Resolver;

@ApplicationScoped
public class RepositoryResolver implements Resolver<Repository> {

    @Inject
    private WebTarget target;

    @ResolveField
	public CompletionStage<RefConnection> refs(Repository repo, Integer last, String owner, String name) {
        return JsonFileHelper.useFiles() ? refsFromFile(repo, last, owner, name) : refsFromApi(repo, last, owner, name);
    }

    private CompletionStage<RefConnection> refsFromApi(Repository repo, Integer last, String owner, String name) {
        return target.path("repos/{owner}/{name}/tags")
                .resolveTemplate("owner", owner)
                .resolveTemplate("name", name)
                .queryParam("per_page", last)
                .request().rx().get(JsonArray.class)
                .thenApply(j -> writeToFile(String.format("tags_%s_%s_%d", owner, name, last), j))
                .thenApply(this::toRefConnection);
    }

    private CompletionStage<RefConnection> refsFromFile(Repository repo, Integer last, String owner, String name) {
        return CompletableFuture.supplyAsync(() ->
        	JsonFileHelper.<JsonArray>readFromFile(String.format("tags_%s_%s_%d", owner, name, last)))
        		.thenApply(this::toRefConnection);
    }

    private RefConnection toRefConnection(JsonArray tags) {
        RefConnection refConnection = new RefConnection();
        List<Ref> refs = tags.stream().map(JsonValue::asJsonObject).map(this::toRef).collect(toList());
        refConnection.setNodes(refs);
        return refConnection;
    }

    private Ref toRef(JsonObject json) {
        Ref ref = new Ref();
        ref.setName(json.getString("name"));
        GitObject target = new Commit();
        target.setOid(json.getJsonObject("commit").getString("sha"));
        ref.setTarget(target);
        return ref;
    }
}
