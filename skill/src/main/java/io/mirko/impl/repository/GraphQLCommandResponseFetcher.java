package io.mirko.impl.repository;

import io.mirko.repository.CommandResponseFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final Logger logger = LoggerFactory.getLogger(GraphQLCommandResponseFetcher.class);

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
        logger.debug("getCommand for commant {}, response: {}", commandId, result);
        if (result == null) {
            return Optional.empty();
        }
        final Optional<List<T>> returnValue = Optional.of((List<T>) result.get("arguments"));

        logger.debug(
                "getCommand for command {}, arguments: {} ({})",
                commandId, result,
                returnValue.map(d -> d.getClass().getName()).orElse("<NULL>")
        );
        graphqlClient.query(
            GraphqlQueries.deleteCommand(),
            Collections.singletonMap("commandId", commandId)
        );

        return returnValue;
    }
}
