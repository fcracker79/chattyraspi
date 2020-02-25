package io.mirko.repository;

public interface CommandSubmitter {
    String submitCommand(String deviceId, CommandType command, Object ... arguments);
}
