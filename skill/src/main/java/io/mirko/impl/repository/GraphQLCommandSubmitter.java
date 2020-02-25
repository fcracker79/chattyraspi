package io.mirko.impl.repository;

import io.mirko.repository.CommandSubmitter;
import io.mirko.repository.CommandType;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;


@ApplicationScoped
@Named
public class GraphQLCommandSubmitter implements CommandSubmitter {
    @Override
    public String submitCommand(String deviceId, CommandType command, Object... arguments) {
        // TODO
        throw new RuntimeException("Implement me");
    }
}
