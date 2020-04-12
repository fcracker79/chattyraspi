package io.mirko.alexa.home.raspberry;


import io.mirko.alexa.home.raspberry.impl.AWSProfile;

public interface UserRepository {
    void saveUser(AWSProfile profile);
}
