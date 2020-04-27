package io.mirko.alexa.home.raspberry;

import java.util.List;
import java.util.UUID;

public interface DeviceRepository {
    UUID registerDevice(String deviceName, String accessToken);
    boolean deleteDevice(UUID deviceId, String accountId);
    List<Device> getDevices(String accountId);
}
