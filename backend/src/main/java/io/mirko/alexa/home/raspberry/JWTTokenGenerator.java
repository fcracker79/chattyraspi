package io.mirko.alexa.home.raspberry;

import java.util.UUID;

public interface JWTTokenGenerator {
    String generateToken(UUID deviceId);
}
