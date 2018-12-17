package org.ops4j.graqulus.cdi.starwars;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import javax.inject.Inject;

import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.ops4j.graqulus.cdi.api.ExecutionRootFactory;
import org.ops4j.graqulus.cdi.impl.GraqulusExtension;

import graphql.ExecutionResult;
import graphql.GraphQL;

@EnableWeld
public class StarWarsServiceTest {

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
            .disableDiscovery()
            .addExtensions(new GraqulusExtension())
            .addPackages(GraqulusExtension.class, StarWarsService.class));

    @Inject
    private ExecutionRootFactory rootFactory;

    @Test
    public void shouldFindOverallHero() {
        GraphQL root = rootFactory.newRoot();
        ExecutionResult result = root.execute("{hero {name}}");
        Map<String, Object> data = result.getData();
        assertThat(data.toString()).isEqualTo("{hero={name=R2-D2}}");
    }

    @Test
    public void shouldFindHeroOfEmpire() {
        GraphQL root = rootFactory.newRoot();
        ExecutionResult result = root.execute("{hero(episode: EMPIRE) {name}}");
        Map<String, Object> data = result.getData();
        assertThat(data.toString()).isEqualTo("{hero={name=Luke Skywalker}}");
    }

    @Test
    public void shouldFindHuman() {
        GraphQL root = rootFactory.newRoot();
        ExecutionResult result = root.execute("{human(id: \"1001\") {name, appearsIn, homePlanet}}");
        Map<String, Object> data = result.getData();
        assertThat(data.toString())
                .isEqualTo("{human={name=Darth Vader, appearsIn=[NEWHOPE, EMPIRE, JEDI], homePlanet=Tatooine}}");
    }

    @Test
    public void shouldNotFindHumanWithIntegerId() {
        GraphQL root = rootFactory.newRoot();
        ExecutionResult result = root.execute("{human(id: 1001) {name, appearsIn, homePlanet}}");
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getMessage()).contains("'IntValue{value=1001}' is not a valid 'String'");
    }

    @Test
    public void shouldFindDroidWithIntegerId() {
        GraphQL root = rootFactory.newRoot();
        ExecutionResult result = root.execute("{droid(id: 2000) {name, primaryFunction}}");
        Map<String, Object> data = result.getData();
        assertThat(data.toString()).isEqualTo("{droid={name=C-3PO, primaryFunction=Protocol}}");
    }

    @Test
    public void shouldFindDroidWithStringId() {
        GraphQL root = rootFactory.newRoot();
        ExecutionResult result = root.execute("{droid(id: \"2000\") {name, primaryFunction}}");
        Map<String, Object> data = result.getData();
        assertThat(data.toString()).isEqualTo("{droid={name=C-3PO, primaryFunction=Protocol}}");
    }

    @Test
    public void shouldFindHeroOfEmpireWithFriends() {
        GraphQL root = rootFactory.newRoot();
        ExecutionResult result = root.execute("{hero(episode: EMPIRE) {name, friends { name }}}");
        Map<String, Object> data = result.getData();
        assertThat(data.toString()).isEqualTo(
                "{hero={name=Luke Skywalker, friends=[{name=Han Solo}, {name=Leia Organa}, {name=C-3PO}, {name=R2-D2}]}}");
    }

    @Test
    public void shouldFindHanWithFriendsAndTheirFathers() {
        GraphQL root = rootFactory.newRoot();
        ExecutionResult result = root
                .execute("{human(id: \"1002\") {name, friends { name, ... on Human { father { name } }}}}");
        Map<String, Object> data = result.getData();
        assertThat(data.toString()).isEqualTo(
                "{human={name=Han Solo, friends=[{name=Luke Skywalker, father={name=Darth Vader}}, {name=Leia Organa, father=null}, {name=R2-D2}]}}");
    }
}
