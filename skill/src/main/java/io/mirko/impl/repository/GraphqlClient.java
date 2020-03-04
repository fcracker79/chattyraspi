package io.mirko.impl.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Named
@ApplicationScoped
public class GraphqlClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @ConfigProperty(name="io.mirko.alexa.home.raspberry.graphql_uri")
    String graphqlUri;

    @Inject
    RSAJWTTokenGenerator jwtTokenGenerator;

    private Map<String, Object> post(Map<String, Object> payload) {
        final Response response =
                ResteasyClientBuilderImpl.newClient().target(graphqlUri)
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", jwtTokenGenerator.generateToken())
                    .post(Entity.entity(payload, MediaType.APPLICATION_JSON));
        if (200 != response.getStatus()) {
            // TODO specialize exceptions
            throw new RuntimeException(
                    String.format("Status %s: %s", response.getStatus(), response.getEntity())
            );
        }
        final InputStream is = (InputStream) response.getEntity();
        try {
            return (Map<String, Object>) OBJECT_MAPPER.readValue(is, Map.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> query(String query, Map<String, Object> variables) {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("query", query);
        if (!variables.isEmpty()) {
            payload.put("variables", variables);
        }
        return post(payload);
    }
}
