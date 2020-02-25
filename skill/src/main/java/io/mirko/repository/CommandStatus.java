package io.mirko.repository;

import java.util.Arrays;
import java.util.Optional;

public enum CommandStatus {
    IGNORED(3),
    FAILED(2),
    TO_BE_EXECUTED(1),
    NOT_FOUND(-1);

    private final int value;
    private CommandStatus(int value) {
        this.value = value;
    }

    public static Optional<CommandStatus> valueOf(int value) {
        return Arrays.stream(values()).filter(d -> d.value == value).findFirst();
    }
}
