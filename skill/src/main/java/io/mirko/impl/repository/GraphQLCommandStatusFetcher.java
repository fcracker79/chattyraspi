package io.mirko.impl.repository;

import io.mirko.repository.CommandStatus;
import io.mirko.repository.CommandStatusFetcher;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@ApplicationScoped
@Named
public class GraphQLCommandStatusFetcher implements CommandStatusFetcher {
    @Override
    public CommandStatus getCommandStatus(String commandId) {
        // TODO
        throw new RuntimeException("Implement me");
    }
}
