package io.mirko.impl.repository;

import io.mirko.repository.CommandResponseFetcher;
import io.mirko.repository.CommandStatus;
import io.mirko.repository.CommandStatusFetcher;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@Named
public class GraphQLCommandResponseFetcher implements CommandResponseFetcher {
    @Inject
    GraphqlClient graphqlClient;

    @Override
    public <T> Optional<T> getCommandResponse(String commandId) {
        Map<String, Object> result = graphqlClient.query(
            GraphqlQueries.fetchCommandResponse(),
            Collections.singletonMap("commandId", commandId)
        );
        //noinspection unchecked
        result = (Map<String, Object>) result.get("data");
        //noinspection unchecked
        result = (Map<String, Object>) result.get("getCommand");
        if (result == null) {
            return Optional.empty();
        }
        final Optional<T> returnValue = Optional.of((T) result.get("arguments"));

        graphqlClient.query(
            GraphqlQueries.deleteCommand(),
            Collections.singletonMap("commandId", commandId)
        );

        return returnValue;
    }
}
