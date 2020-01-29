package io.mirko.alexa.home.raspberry;

public interface DeviceRepository {
    void registerDevice(String deviceId, String accessToken);
}
