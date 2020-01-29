package io.mirko.alexa.home.raspberry.exceptions;

public class AlexaRaspberryException extends RuntimeException {
    public AlexaRaspberryException() {
    }

    public AlexaRaspberryException(String message) {
        super(message);
    }

    public AlexaRaspberryException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlexaRaspberryException(Throwable cause) {
        super(cause);
    }

    public AlexaRaspberryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
