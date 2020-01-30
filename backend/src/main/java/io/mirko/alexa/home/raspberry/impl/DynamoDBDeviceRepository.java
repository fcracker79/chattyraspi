package io.mirko.alexa.home.raspberry.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
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
public class DynamoDBDeviceRepository implements DeviceRepository {
    @Inject
    AmazonDynamoDB dynamoDB;

    @ConfigProperty(name="io.mirko.alexa.home.raspberry.devices_table")
    String devicesTable;

    @Inject
    @RestClient
    AWSProfileService awsProfileService;

    @Override
    public void registerDevice(String deviceId, String accessToken) {
        final Map<String, AttributeValue> row = new HashMap<>();
        row.put("device_id", new AttributeValue(deviceId));
        // e.g. {"user_id":"amzn1.account.AE...RQ","name":"John Burns","email":"john@burns.com"}'
        row.put("aws_id", new AttributeValue(awsProfileService.getProfile(accessToken).user_id));
        dynamoDB.putItem(devicesTable, row);
    }
}
