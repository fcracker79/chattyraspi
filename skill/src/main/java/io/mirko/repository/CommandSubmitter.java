package io.mirko.repository;

import java.util.UUID;

public interface CommandSubmitter {
    String submitCommand(UUID deviceId, CommandType command, Object ... arguments);
}
