package io.mirko.impl;

public class AWSProfile {
    public String user_id;
    public String name;
    public String email;


    @Override
    public String toString() {
        return "AWSProfile{" +
                "user_id='" + user_id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
