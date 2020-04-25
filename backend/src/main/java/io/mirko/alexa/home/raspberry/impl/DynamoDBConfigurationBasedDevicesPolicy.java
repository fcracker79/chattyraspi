package io.mirko.alexa.home.raspberry.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import io.mirko.alexa.home.raspberry.DeviceCreationPolicy;
import io.mirko.alexa.home.raspberry.DeviceRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@Named
public class DynamoDBConfigurationBasedDevicesPolicy implements DeviceCreationPolicy {
    @Inject
    @RestClient
    AWSProfileService awsProfileService;

    @Inject
    AmazonDynamoDB dynamoDB;

    @ConfigProperty(name="io.mirko.alexa.home.raspberry.configuration.tables.configuration_table")
    String configurationTable;

    @ConfigProperty(name="io.mirko.alexa.home.raspberry.configuration.tables.global_configuration_table")
    String globalConfigurationTable;

    @Inject
    DeviceRepository deviceRepository;

    public boolean canCreateNewDevice(String accountToken) {
        final String accountId = awsProfileService.getProfile(accountToken).user_id;
        return Integer.parseInt(findConfiguration(
                accountId, ConfigurationEnum.MAX_NUMBER_OF_DEVICES
        )) >= deviceRepository.getDevices(accountId).size();
    }

    private String findConfiguration(String accountId, ConfigurationEnum configuration) {
        final Map<String, AttributeValue> row = new HashMap<>();
        row.put("aws_id", new AttributeValue(accountId));
        row.put("configuration_key", new AttributeValue(configuration.value));
        GetItemResult result = dynamoDB.getItem(configurationTable, row);
        if (result == null || result.getItem() == null) {
            row.remove("aws_id");
            result = dynamoDB.getItem(globalConfigurationTable, row);
            if (result == null || result.getItem() == null) {
                row.put("configuration_value", new AttributeValue(configuration.defaultValue));
                dynamoDB.putItem(globalConfigurationTable, row);
                return configuration.defaultValue;
            }
        }
        return result.getItem().get("configuration_value").getS();
    }

    private enum ConfigurationEnum {
        MAX_NUMBER_OF_DEVICES("max_number_of_devices", "10");

        private final String value;
        private final String defaultValue;
        ConfigurationEnum(String value, String defaultValue) {
            this.value = value;
            this.defaultValue = defaultValue;
        }
    }
}
