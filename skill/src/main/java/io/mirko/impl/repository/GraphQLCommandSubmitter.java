package io.mirko.impl.repository;

import io.mirko.repository.CommandSubmitter;
import io.mirko.repository.CommandType;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


@ApplicationScoped
@Named
public class GraphQLCommandSubmitter implements CommandSubmitter {
    @Inject
    GraphqlClient graphqlClient;

    @Override
    public String submitCommand(String deviceId, CommandType command, Object... arguments) {
        final Map<String, Object> newCommand = new HashMap<>();
        newCommand.put("command", command.value());
        newCommand.put("arguments", arguments);
        newCommand.put("deviceId", deviceId);
        Map<String, Object> result = graphqlClient.query(
                GraphqlQueries.submitCommand(),
                Collections.singletonMap("command", newCommand)
        );
        //noinspection unchecked
        result = (Map<String, Object>) result.get("data");
        //noinspection unchecked
        result = (Map<String, Object>) result.get("submitCommand");
        return (String) result.get("commandId");
    }
}
