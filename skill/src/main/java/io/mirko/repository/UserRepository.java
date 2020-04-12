package io.mirko.repository;

import io.mirko.impl.AWSProfile;

public interface UserRepository {
    void saveUser(AWSProfile profile);
}
