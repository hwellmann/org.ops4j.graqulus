package org.ops4j.graqulus.starwars;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;

import graphql.TypeResolutionEnvironment;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.idl.RuntimeWiring;

public class StarWarsWiring {

    public RuntimeWiring buildRuntimeWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type("Query", t -> t
                        .dataFetcher("hero", this::findHero)
                        .dataFetcher("human", this::findHuman)
                        .dataFetcher("droid", this::findDroid))
                .type(newTypeWiring("Character")
                        .typeResolver(this::resolveCharacter))
                .type(newTypeWiring("Droid")
                        .dataFetcher("friends", this::findFriends))
                .type(newTypeWiring("Human")
                        .dataFetcher("friends", this::findFriends))
                .build();
    }

    private Character findHero(DataFetchingEnvironment env) {
        Episode episode = env.getArgument("episode");        
        return StarWarsData.findHero(episode);
    }

    private Human findHuman(DataFetchingEnvironment env) {
        String id = env.getArgument("id");
        return StarWarsData.findHuman(id);

    }

    private Droid findDroid(DataFetchingEnvironment env) {
        String id = env.getArgument("id");
        return StarWarsData.findDroid(id);
    }
    
    private GraphQLObjectType resolveCharacter(TypeResolutionEnvironment env) {
        String typeName = env.getObject().getClass().getSimpleName();
        return env.getSchema().getObjectType(typeName);
    }
    
    private CompletableFuture<List<Character>> findFriends(DataFetchingEnvironment env) {
        Character source = env.getSource();
        List<String> ids = source.getFriends().stream().map(Character::getId).collect(toList());
        DataLoader<String, Character> dataLoader = env.getDataLoader(Character.class.getSimpleName());
        if (dataLoader == null) {
            return CompletableFuture.completedFuture(ids.stream().map(StarWarsData::findCharacter).collect(toList()));
        }
        return dataLoader.loadMany(ids);
    }
}
