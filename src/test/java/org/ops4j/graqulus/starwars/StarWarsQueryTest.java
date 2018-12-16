package org.ops4j.graqulus.starwars;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

@SuppressWarnings("unchecked")
public class StarWarsQueryTest {
    
    private GraphQL queryRoot;

    @BeforeEach
    public void before() {
        SchemaParser parser = new SchemaParser();
        TypeDefinitionRegistry registry = parser.parse(new File("src/test/resources/starWarsSchemaAnnotated.graphqls"));
        SchemaGenerator generator = new SchemaGenerator();
        GraphQLSchema schema = generator.makeExecutableSchema(registry, new StarWarsWiring().buildRuntimeWiring());
        queryRoot = GraphQL.newGraphQL(schema).build();
    }

    @Test
    public void artooIsHero() {
        String query = "query HeroNameQuery {\n" + 
                "          hero {\n" + 
                "            name\n" + 
                "          }\n" + 
                "        }";
        
        ExecutionInput input = ExecutionInput.newExecutionInput().query(query).build();
        ExecutionResult result = queryRoot.execute(input);
        assertThat(result.<Object>getData()).isNotNull();
        
        Map<String, Object> data = result.getData();
        assertThat(data.get("hero")).isInstanceOf(Map.class);
        Map<String, Object> hero = (Map<String, Object>) data.get("hero");
        assertThat(hero.get("name")).isEqualTo("R2-D2");
    }
    
    @Test
    public void artooWithFriends() {
        String query = 
                "        query HeroNameAndFriendsQuery {\n" + 
                "            hero {\n" + 
                "                id\n" + 
                "                name\n" + 
                "                friends {\n" + 
                "                    name\n" + 
                "                }\n" + 
                "            }\n" + 
                "        }\n" + 
                "";
        
        ExecutionInput input = ExecutionInput.newExecutionInput().query(query).build();
        ExecutionResult result = queryRoot.execute(input);
        assertThat(result.<Object>getData()).isNotNull();
        
        Map<String, Object> data = result.getData();
        assertThat(data.get("hero")).isInstanceOf(Map.class);
        Map<String, Object> hero = (Map<String, Object>) data.get("hero");
        assertThat(hero.get("name")).isEqualTo("R2-D2");
    }

    @Test
    public void nestedQuery() {
        String query = 
                "        query NestedQuery {\n" + 
                "            hero {\n" + 
                "                name\n" + 
                "                friends {\n" + 
                "                    name\n" + 
                "                    appearsIn\n" + 
                "                    friends {\n" + 
                "                        name\n" + 
                "                    }\n" + 
                "                }\n" + 
                "            }\n" + 
                "        }";
        
        ExecutionInput input = ExecutionInput.newExecutionInput().query(query).build();
        ExecutionResult result = queryRoot.execute(input);
        assertThat(result.<Object>getData()).isNotNull();
        
        Map<String, Object> data = result.getData();
        assertThat(data.get("hero")).isInstanceOf(Map.class);
        Map<String, Object> hero = (Map<String, Object>) data.get("hero");
        assertThat(hero.get("name")).isEqualTo("R2-D2");
    }
    
    @Test
    public void nestedQueryBatched() {
        String query = 
                "        query NestedQuery {\n" + 
                "            hero {\n" + 
                "                name\n" + 
                "                friends {\n" + 
                "                    name\n" + 
                "                    appearsIn\n" + 
                "                    friends {\n" + 
                "                        name\n" + 
                "                    }\n" +
                "                }\n" + 
                "            }\n" + 
                "        }";
        
        DataLoader<String, Character> characterDataLoader = DataLoader.newDataLoader(this::loadCharacters);        
        
        DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry()
                .register(Character.class.getSimpleName(), characterDataLoader);
        
        ExecutionInput input = ExecutionInput.newExecutionInput()
                .dataLoaderRegistry(dataLoaderRegistry)
                .query(query).build();
        ExecutionResult result = queryRoot.execute(input);
        assertThat(result.<Object>getData()).isNotNull();
        
        Map<String, Object> data = result.getData();
        assertThat(data.get("hero")).isInstanceOf(Map.class);
        Map<String, Object> hero = (Map<String, Object>) data.get("hero");
        assertThat(hero.get("name")).isEqualTo("R2-D2");
    }
    
    private CompletionStage<List<Character>> loadCharacters(List<String> ids) {
        return CompletableFuture.supplyAsync(() -> ids.stream().map(StarWarsData::findCharacter).collect(toList()));
    }
    
}
