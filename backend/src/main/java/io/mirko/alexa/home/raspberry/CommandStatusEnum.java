package io.mirko.alexa.home.raspberry;

public enum CommandStatusEnum {
    SUBMITTED(1),
    FAILED(2),
    IGNORED(3);
    private final int status;
    private CommandStatusEnum(int status) {
        this.status = status;
    }

    public int intValue() {
        return status;
    }
}
