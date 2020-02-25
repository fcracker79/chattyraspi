package io.mirko.repository;

public interface CommandStatusFetcher {
    CommandStatus getCommandStatus(String commandId);
}
