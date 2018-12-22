package org.ops4j.graqulus.github;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLScalarType.newScalar;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;
import static java.util.stream.Collectors.toList;
import static org.ops4j.graqulus.github.JsonFileHelper.readFromFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.inject.spi.DefinitionException;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderOptions;
import org.dataloader.DataLoaderRegistry;
import org.dataloader.impl.CompletableFutureKit;
import org.junit.jupiter.api.Test;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.TypeResolutionEnvironment;
import graphql.execution.ExecutionStepInfo;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.RuntimeWiring.Builder;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

public class GitHubQueryTest {

	private TypeDefinitionRegistry registry;
	private RuntimeWiring runtimeWiring;

	@Test
	public void shouldGetTagsWithCommits() {
		String query = "{\n" +
				"  repository(owner: \"wildfly\", name: \"wildfly-core\") {\n" +
				"    refs(refPrefix: \"refs/tags/\", last: 5) {\n" +
				"      nodes {\n" +
				"        target {\n" +
				"          oid\n" +
				"          ... on Commit {\n" +
				"            message\n" +
				"            committedDate\n" +
				"            changedFiles\n" +
				"            committer {\n" +
				"              user {\n" +
				"                login\n" +
				"              }\n" +
				"            }\n" +
				"          }\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

		loadAndParseSchema();
		buildRuntimeWiring();

        SchemaGenerator generator = new SchemaGenerator();
        GraphQLSchema schema = generator.makeExecutableSchema(registry, runtimeWiring);
        GraphQL queryRoot = GraphQL.newGraphQL(schema).build();


        DataLoader<String, Commit> commitDataLoader = DataLoader.newDataLoader(this::loadCommits, DataLoaderOptions.newOptions().setBatchingEnabled(false));

        DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry()
                .register(Commit.class.getSimpleName(), commitDataLoader);

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
        		.dataLoaderRegistry(dataLoaderRegistry)
        		.query(query)
        		.build();

        ExecutionResult result = queryRoot.execute(executionInput);
        Map<String, Object> data = result.getData();
        System.out.println(data);

	}

	private void loadAndParseSchema() {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		String schemaPath = "githubPartial.graphqls";
		InputStream is = tccl.getResourceAsStream(schemaPath);
		if (is == null) {
			throw new DefinitionException("No schema resource with path " + schemaPath);
		}

		try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
			SchemaParser parser = new SchemaParser();
			registry = parser.parse(reader);
		} catch (IOException exc) {
			throw new DefinitionException(exc);
		}
	}

	private void buildRuntimeWiring() {
        Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring()
        		.type(newTypeWiring("Query")
        				.dataFetcher("repository", this::findRepository))
        		.type(newTypeWiring("Repository")
        				.dataFetcher("refs", this::findRefConnection))
        		.type(newTypeWiring("Ref")
        				.dataFetcher("target", this::findCommit))
        		.type(newTypeWiring("GitObject").typeResolver(this::resolveGitObject))
        		.type(newTypeWiring("Node").typeResolver(this::resolveNode))
        		.scalar(newScalar(GraphQLString).name("GitObjectID").build())
        		.scalar(newScalar(GraphQLString).name("GitTimestamp").build())
        		.scalar(newScalar(GraphQLString).name("HTML").build())
        		.scalar(newScalar(GraphQLString).name("URI").build())
        		.scalar(newScalar(GraphQLString).name("DateTime").build())
        		;

        runtimeWiring = runtimeWiringBuilder.build();
    }

	private Repository findRepository(DataFetchingEnvironment env) {
		String owner = env.getArgument("owner");
		String name = env.getArgument("name");

		JsonObject json = readFromFile(String.format("repo_%s_%s", owner, name));

		if (json == null) {
			return null;
		}
		Repository repo = new Repository();
		repo.setCreatedAt(json.getString("created_at"));
		repo.setName(name);
		repo.setUrl(json.getString("clone_url"));
		return repo;
	}

	private CompletionStage<RefConnection> findRefConnection(DataFetchingEnvironment env) {
		int last = env.getArgument("last");

		String owner = findArgumentOnStack("owner", env);
		String name = findArgumentOnStack("name", env);
		return CompletableFuture
				.supplyAsync(
						() -> JsonFileHelper.<JsonArray>readFromFile(String.format("tags_%s_%s_%d", owner, name, last)))
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

	private GraphQLObjectType resolveGitObject(TypeResolutionEnvironment env) {
		return env.getSchema().getObjectType("Commit");
	}

	private GraphQLObjectType resolveNode(TypeResolutionEnvironment env) {
		return env.getSchema().getObjectType(env.getObject().getClass().getSimpleName());
	}

	private CompletionStage<Commit> findCommit(DataFetchingEnvironment env) {
		Ref ref = env.getSource();
		GitObject target = ref.getTarget();

		Map<String, String> context = new HashMap<>();
        String owner = findArgumentOnStack("owner", env);
        String name = findArgumentOnStack("name", env);
        context.put("owner", owner);
        context.put("name", name);

        DataLoader<String, Commit> dataLoader = env.getDataLoader(Commit.class.getSimpleName());
		return dataLoader.load(target.getOid(), context);
	}

	private CompletionStage<Commit> findCommit(String owner, String name, String id) {
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

    CompletionStage<List<Commit>> loadCommits(List<String> keys, BatchLoaderEnvironment environment) {
    	Map<?, ?> keyContexts = environment.getKeyContexts();
    	List<CompletionStage<Commit>> commits = keys.stream().map(id -> findCommit(id, keyContexts)).collect(toList());
    	return CompletableFutureKit.allOf(commits.stream().map(CompletionStage::toCompletableFuture).collect(toList()));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	CompletionStage<Commit> findCommit(String id, Map keyContexts) {
    	Map<String, String> context = (Map<String, String>) keyContexts.get(id);
    	String owner = context.get("owner");
    	String name = context.get("name");
    	return findCommit(owner, name, id);
    }

	private <T> T findArgumentOnStack(String name, DataFetchingEnvironment env) {
	    T arg = env.getArgument(name);
	    if (arg == null) {
	        ExecutionStepInfo parent = env.getExecutionStepInfo().getParent();
	        if (parent != null) {
	        	arg = findArgumentOnStack(name, parent);
	        }
	    }
	    return arg;
	}

	private <T> T findArgumentOnStack(String name, ExecutionStepInfo info) {
        T arg = info.getArgument(name);
        if (arg == null) {
            if (info.getParent() != null) {
            	arg = findArgumentOnStack(name, info.getParent());
            }
        }
        return arg;
	}
}
