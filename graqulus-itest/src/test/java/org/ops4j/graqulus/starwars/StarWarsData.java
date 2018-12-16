package org.ops4j.graqulus.starwars;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.ops4j.graqulus.starwars.Episode.EMPIRE;
import static org.ops4j.graqulus.starwars.Episode.JEDI;
import static org.ops4j.graqulus.starwars.Episode.NEWHOPE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class StarWarsData {

    public static Human luke = new Human();
    public static Human vader = new Human();
    public static Human han = new Human();
    public static Human leia = new Human();
    public static Human tarkin = new Human();
    public static Droid threepio = new Droid();
    public static Droid artoo = new Droid();

    private static Map<Episode, Character> heroes = new HashMap<>();
    private static List<Human> humans = Arrays.asList(luke, vader, han, leia, tarkin);
    private static List<Droid> droids = Arrays.asList(threepio, artoo);
    private static Map<String, Character> characters;

    static {
        luke.setId("1000");
        luke.setName("Luke Skywalker");
        luke.setFriends(Arrays.asList(han, leia, threepio, artoo));
        luke.setAppearsIn(Arrays.asList(NEWHOPE, EMPIRE, JEDI));
        luke.setHomePlanet("Tatooine");

        vader.setId("1001");
        vader.setName("Darth Vader");
        vader.setFriends(Arrays.asList(tarkin));
        vader.setAppearsIn(Arrays.asList(NEWHOPE, EMPIRE, JEDI));
        vader.setHomePlanet("Tatooine");

        han.setId("1002");
        han.setName("Han Solo");
        han.setFriends(Arrays.asList(luke, leia, artoo));
        han.setAppearsIn(Arrays.asList(NEWHOPE, EMPIRE, JEDI));

        leia.setId("1003");
        leia.setName("Leia Organa");
        leia.setFriends(Arrays.asList(luke, han, threepio, artoo));
        leia.setAppearsIn(Arrays.asList(NEWHOPE, EMPIRE, JEDI));
        leia.setHomePlanet("Alderaan");

        tarkin.setId("1004");
        tarkin.setName("Wilhuff Tarkin");
        tarkin.setFriends(Arrays.asList(vader));
        tarkin.setAppearsIn(Arrays.asList(NEWHOPE));

        threepio.setId("2000");
        threepio.setName("C-3PO");
        threepio.setFriends(Arrays.asList(luke, han, leia, artoo));
        threepio.setAppearsIn(Arrays.asList(NEWHOPE, EMPIRE, JEDI));
        threepio.setPrimaryFunction("Protocol");

        artoo.setId("2001");
        artoo.setName("R2-D2");
        artoo.setFriends(Arrays.asList(luke, han, threepio));
        artoo.setAppearsIn(Arrays.asList(NEWHOPE, EMPIRE, JEDI));
        artoo.setPrimaryFunction("Astromech");

        heroes.put(NEWHOPE, artoo);
        heroes.put(EMPIRE, luke);
        heroes.put(JEDI, artoo);

        characters = Stream.concat(humans.stream(), droids.stream()).collect(toMap(Character::getId, identity()));
    }

    public static Character findHero(Episode episode) {
        return heroes.getOrDefault(episode, artoo);
    }

    public static Human findHuman(String id) {
        return humans.stream().filter(h -> h.getId().equals(id)).findFirst().orElse(null);
    }

    public static Droid findDroid(String id) {
        return droids.stream().filter(d -> d.getId().equals(id)).findFirst().orElse(null);
    }

    public static Character findCharacter(String id) {
        System.out.println("findCharacter id = " + id);
        return characters.get(id);
    }
}
