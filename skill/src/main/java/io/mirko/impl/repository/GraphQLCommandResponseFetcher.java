package io.mirko.impl.repository;

import io.mirko.repository.CommandResponseFetcher;
import io.mirko.repository.CommandStatus;
import io.mirko.repository.CommandStatusFetcher;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
@Named
public class GraphQLCommandResponseFetcher implements CommandResponseFetcher {
    @Inject
    GraphqlClient graphqlClient;

    @Override
    public <T> Optional<List<T>> getCommandResponse(String commandId) {
        Map<String, Object> result = graphqlClient.query(
            GraphqlQueries.fetchCommandResponse(),
            Collections.singletonMap("commandId", commandId)
        );
        //noinspection unchecked
        result = (Map<String, Object>) result.get("data");
        //noinspection unchecked
        result = (Map<String, Object>) result.get("getCommand");
        System.out.format("getCommand for commant %s, response: %s\n", commandId, result);
        if (result == null) {
            return Optional.empty();
        }
        final Optional<List<T>> returnValue = Optional.of((List<T>) result.get("arguments"));

        List<T> t = returnValue.orElse(null);
        System.out.format(
                "getCommand for command %s, arguments: %s (%s)\n",
                commandId, t, t == null ? "<NULL>" : t.getClass().getName()
        );
        graphqlClient.query(
            GraphqlQueries.deleteCommand(),
            Collections.singletonMap("commandId", commandId)
        );

        return returnValue;
    }
}
