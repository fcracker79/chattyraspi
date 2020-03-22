package io.mirko.alexa.home.raspberry;

public interface DeviceRepository {
    void registerDevice(String deviceId, String accessToken);
    boolean isValidDevice(String deviceId, String userId);
    boolean existsDevice(String deviceId);
    boolean deleteDevice(String deviceId, String accountId);
    Iterable<Device> getDevices(String accountId);
}
