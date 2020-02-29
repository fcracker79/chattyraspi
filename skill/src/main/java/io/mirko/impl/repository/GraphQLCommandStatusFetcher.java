package io.mirko.impl.repository;

import io.mirko.repository.CommandStatus;
import io.mirko.repository.CommandStatusFetcher;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.Map;

@ApplicationScoped
@Named
public class GraphQLCommandStatusFetcher implements CommandStatusFetcher {
    @Inject
    GraphqlClient graphqlClient;

    @Override
    public CommandStatus getCommandStatus(String commandId) {
        Map<String, Object> result = graphqlClient.query(
            GraphqlQueries.fetchCommandStatus(),
                Collections.singletonMap("commandId", commandId)
        );
        //noinspection unchecked
        result = (Map<String, Object>) result.get("data");
        //noinspection unchecked
        result = (Map<String, Object>) result.get("getCommand");
        if (result == null) {
            return CommandStatus.NOT_FOUND;
        }
        return CommandStatus.valueOf((Integer) result.get("status")).orElseThrow(RuntimeException::new);
    }
}
