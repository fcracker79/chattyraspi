package io.mirko.repository;

public interface DevicesFetcher {
    Iterable<Device> getDevices(String accountId);
}
