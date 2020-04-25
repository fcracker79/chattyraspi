package io.mirko.alexa.home.raspberry;

import java.util.List;

public interface DeviceRepository {
    void registerDevice(String deviceId, String accessToken);
    boolean isValidDevice(String deviceId, String userId);
    boolean existsDevice(String deviceId);
    boolean deleteDevice(String deviceId, String accountId);
    List<Device> getDevices(String accountId);
}
