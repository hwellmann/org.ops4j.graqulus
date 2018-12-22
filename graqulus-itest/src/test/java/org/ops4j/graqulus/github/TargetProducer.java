package org.ops4j.graqulus.github;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.jboss.resteasy.client.jaxrs.BasicAuthentication;

@ApplicationScoped
public class TargetProducer {

    @Produces
    @ApplicationScoped
    WebTarget target() {
        Client client = ClientBuilder.newClient();

        // insert your own credentials
        client.register(new BasicAuthentication("username", "password"));

        return client.target("https://api.github.com");
    }
}
