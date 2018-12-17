package org.ops4j.graqulus.cdi.starwars;

import static java.util.stream.Collectors.toList;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.ops4j.graqulus.cdi.api.BatchLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StarWarsRepository {
    
    private static Logger log = LoggerFactory.getLogger(StarWarsRepository.class);

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
        log.debug("findCharacter id = {}", id);
        return StarWarsData.characters.get(id);
    }

    @BatchLoader
    public List<Character> findCharacters(List<String> ids) {
        log.debug("findCharacters ids = {}", ids);
        return ids.stream().map(this::findCharacter).collect(toList());
    }
    
    @BatchLoader
    public List<Human> findHumans(List<String> ids) {
        log.debug("findHumans ids = {}", ids);
        return ids.stream().map(this::findHuman).collect(toList());
    }
    
}
