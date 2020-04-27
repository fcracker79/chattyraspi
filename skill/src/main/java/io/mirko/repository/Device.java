package io.mirko.repository;

import java.util.UUID;

public final class Device {
    public final UUID deviceId;
    public final String deviceName;
    public Device(UUID deviceId, String deviceName) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
    }
}
