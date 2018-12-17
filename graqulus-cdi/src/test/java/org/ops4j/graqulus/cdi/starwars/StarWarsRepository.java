package org.ops4j.graqulus.cdi.starwars;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StarWarsRepository {

	public Character findHero(Episode episode) {
        return StarWarsData.heroes.getOrDefault(episode, StarWarsData.artoo);
    }

    public Human findHuman(String id) {
        return StarWarsData.humans.stream().filter(h -> h.getId().equals(id)).findFirst().orElse(null);
    }

    public Droid findDroid(String id) {
        return StarWarsData.droids.stream().filter(d -> d.getId().equals(id)).findFirst().orElse(null);
    }

    public Character findCharacter(String id) {
        System.out.println("findCharacter id = " + id);
        return StarWarsData.characters.get(id);
    }
}
