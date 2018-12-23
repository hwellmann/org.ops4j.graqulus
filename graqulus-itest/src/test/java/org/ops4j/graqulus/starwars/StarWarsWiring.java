package org.ops4j.graqulus.starwars;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;
import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;

import graphql.TypeResolutionEnvironment;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring;

public class StarWarsWiring {

	private static GraphQLScalarType DATE = GraphQLScalarType.newScalar().name("Date")
			.coercing(new Coercing<LocalDate, String>() {

				@Override
				public String serialize(Object dataFetcherResult) throws CoercingSerializeException {

					return ((LocalDate) dataFetcherResult).toString();
				}

				@Override
				public LocalDate parseValue(Object input) throws CoercingParseValueException {
					try {
						return LocalDate.parse(input.toString(), DateTimeFormatter.ISO_LOCAL_DATE);
					} catch (DateTimeParseException exc) {
						throw new CoercingParseValueException(exc);
					}
				}

				@Override
				public LocalDate parseLiteral(Object input) throws CoercingParseLiteralException {
					if (input instanceof StringValue) {
						StringValue stringValue = (StringValue) input;
						try {
							return LocalDate.parse(stringValue.getValue());
						} catch (DateTimeParseException exc) {
							throw new CoercingParseLiteralException(exc);
						}
					}
					throw new CoercingParseLiteralException("Cannot parse literal " + input);
				}

    }).build();

    public RuntimeWiring buildRuntimeWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type("Query", t -> t
                        .dataFetcher("hero", this::findHero)
                        .dataFetcher("human", this::findHuman)
                        .dataFetcher("droid", this::findDroid)
                        .dataFetcher("launchDate", this::findLaunchDate)
                		.dataFetcher("launchedAfter", this::findLaunchedAfter))
                .type(newTypeWiring("Character").typeResolver(this::resolveCharacter))
                .type(newTypeWiring("Droid").dataFetcher("friends", this::findFriends))
                .type(newTypeWiring("Human").dataFetcher("friends", this::findFriends))
                .scalar(DATE)
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

    private LocalDate findLaunchDate(DataFetchingEnvironment env) {
        Episode episode = env.getArgument("episode");
        return StarWarsData.findLaunchDate(episode);
    }

    private List<Episode> findLaunchedAfter(DataFetchingEnvironment env) {
        LocalDate date = env.getArgument("date");
        return StarWarsData.findLaunchedAfter(date);
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
