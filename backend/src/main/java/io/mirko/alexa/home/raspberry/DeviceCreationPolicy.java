package io.mirko.alexa.home.raspberry;

public interface DeviceCreationPolicy {
    boolean canCreateNewDevice(String accountToken);
}
