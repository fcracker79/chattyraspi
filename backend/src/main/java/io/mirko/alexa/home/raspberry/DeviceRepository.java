package io.mirko.alexa.home.raspberry;

public interface DeviceRepository {
    void registerDevice(String deviceId, String accessToken);
    boolean isValidDevice(String deviceId, String userId);
    boolean existsDevice(String deviceId);
    Iterable<Device> getDevices(String accountId);
}
