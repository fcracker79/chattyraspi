package io.mirko.impl.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Named
@ApplicationScoped
public class GraphqlClient {
    private final Logger logger = LoggerFactory.getLogger(GraphqlClient.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private Invocation.Builder client;
    @ConfigProperty(name="io.mirko.alexa.home.raspberry.graphql_uri")
    String graphqlUri;

    @Inject
    RSAJWTTokenGenerator jwtTokenGenerator;

    @PostConstruct
    public void init() {
        client =  ResteasyClientBuilderImpl.newClient().target(graphqlUri)
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", jwtTokenGenerator.generateToken());
    }

    private Map<String, Object> post(Map<String, Object> payload) {
        logger.debug("Query {} execution", payload);
        final Response response = client.post(Entity.entity(payload, MediaType.APPLICATION_JSON));
        if (200 != response.getStatus()) {
            // TODO specialize exceptions
            throw new RuntimeException(
                    String.format("Status %s: %s", response.getStatus(), response.getEntity())
            );
        }
        logger.debug("Query execution successful {}", payload);
        final InputStream is = (InputStream) response.getEntity();
        try {
            Map<String, Object> result = (Map<String, Object>) OBJECT_MAPPER.readValue(is, Map.class);
            logger.debug("Query returned {}", result);
            if (result.get("errors") != null) {
                throw new GraphqlException((List<Map<String, Object>>) result.get("errors"));
            }
            return result;
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

    public static class GraphqlException extends RuntimeException {
        public final List<Map<String, Object>> errors;
        public GraphqlException(List<Map<String, Object>> errors) {
            super(errors.toString());
            this.errors = errors;
        }
    }
}
