package org.ops4j.graqulus.cdi.starwars;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.ops4j.graqulus.cdi.api.Query;
import org.ops4j.graqulus.cdi.api.Schema;

@Dependent
@Schema(path = "starWars.graphqls", modelPackage = "org.ops4j.graqulus.cdi.starwars")
public class StarWarsService implements org.ops4j.graqulus.cdi.starwars.Query {

	@Inject
	private StarWarsRepository repository;

	@Override
    @Query
	public Character hero(Episode episode) {
		return repository.findHero(episode);
	}

	@Override
    @Query
	public Human human(String id) {
		return repository.findHuman(id);
	}

	@Override
    @Query
	public Droid droid(String id) {
		return repository.findDroid(id);
	}

}
