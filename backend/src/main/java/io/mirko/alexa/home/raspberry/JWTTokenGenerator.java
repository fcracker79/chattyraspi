package io.mirko.alexa.home.raspberry;

public interface JWTTokenGenerator {
    String generateToken(String deviceId);
}
