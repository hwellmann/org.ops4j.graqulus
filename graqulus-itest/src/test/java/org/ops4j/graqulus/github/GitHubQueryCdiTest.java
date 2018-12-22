package org.ops4j.graqulus.github;

import java.util.Map;

import javax.inject.Inject;

import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.ops4j.graqulus.cdi.api.ExecutionRoot;
import org.ops4j.graqulus.cdi.api.ExecutionRootFactory;
import org.ops4j.graqulus.cdi.impl.GraqulusExtension;

import graphql.ExecutionResult;

@EnableWeld
public class GitHubQueryCdiTest {

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
            .disableDiscovery()
            .addExtensions(new GraqulusExtension())
            .addPackages(GraqulusExtension.class, GitHubService.class));

    @Inject
    private ExecutionRootFactory rootFactory;

	@Test
	public void shouldGetTagsWithCommits() {
		String query = "{\n" +
				"  repository(owner: \"wildfly\", name: \"wildfly-core\") {\n" +
				"    refs(refPrefix: \"refs/tags/\", last: 5) {\n" +
				"      nodes {\n" +
				"        target {\n" +
				"          oid\n" +
				"          ... on Commit {\n" +
				"            message\n" +
				"            committedDate\n" +
				"            changedFiles\n" +
				"            committer {\n" +
				"              user {\n" +
				"                login\n" +
				"              }\n" +
				"            }\n" +
				"          }\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";

        ExecutionRoot queryRoot = rootFactory.newRoot();
        ExecutionResult result = queryRoot.execute(query);
        Map<String, Object> data = result.getData();
        System.out.println(data);

	}

}
