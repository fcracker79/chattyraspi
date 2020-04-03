package io.mirko.repository;

import java.util.List;
import java.util.Optional;

public interface CommandResponseFetcher {
    <T> Optional<List<T>> getCommandResponse(String commandId);
}
