package io.mirko.alexa.home.raspberry;

public interface DeviceRepository {
    void registerDevice(String deviceId, String accessToken);
    boolean isValidDevice(String deviceId, String userId);
}
