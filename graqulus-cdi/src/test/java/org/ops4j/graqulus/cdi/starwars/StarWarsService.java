package org.ops4j.graqulus.cdi.starwars;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.ops4j.graqulus.cdi.api.RootOperation;
import org.ops4j.graqulus.cdi.api.Schema;

@Dependent
@Schema(path = "starWars.graphqls", modelPackage = "org.ops4j.graqulus.cdi.starwars")
@RootOperation
public class StarWarsService implements Query {

	@Inject
	private StarWarsRepository repository;

	@Override
	public Character hero(Episode episode) {
		return repository.findHero(episode);
	}

	@Override
	public Human human(String id) {
		return repository.findHuman(id);
	}

	@Override
	public Droid droid(String id) {
		return repository.findDroid(id);
	}
}
