package io.mirko.repository;

import java.util.Optional;

public interface CommandResponseFetcher {
    <T> Optional<T> getCommandResponse(String commandId);
}
