package org.ops4j.graqulus.cdi.starwars;

import static java.util.stream.Collectors.toList;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.ops4j.graqulus.cdi.api.BatchLoader;

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

    @BatchLoader
    public List<Character> findCharacters(List<String> ids) {
        System.out.println("findCharacter ids = " + ids);
        return ids.stream().map(this::findCharacter).collect(toList());
    }
}
