package io.mirko.repository;

public enum CommandType {
    TURN_ON("turnOn"),
    TURN_OFF("turnOff");

    private final String value;
    private CommandType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
