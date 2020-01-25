package com.amazon.ask.model.services.util;

public class UserAgentHelper {
    private final String sdkVersion;
    private UserAgentHelper(Builder builder) {
        this.sdkVersion = builder.sdkVersion;
    }

    public String getUserAgent() {
        return "curl/7.58.0";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String sdkVersion;
        public UserAgentHelper build() {
            return new UserAgentHelper(this);
        }

        public Builder withSdkVersion(String sdkVersion) {
            this.sdkVersion = sdkVersion;
            return this;
        }
    }
}
