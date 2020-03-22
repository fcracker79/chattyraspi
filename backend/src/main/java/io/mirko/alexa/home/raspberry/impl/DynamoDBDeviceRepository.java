package io.mirko.alexa.home.raspberry.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import io.mirko.alexa.home.raspberry.Device;
import io.mirko.alexa.home.raspberry.DeviceRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@ApplicationScoped
@Named
public class DynamoDBDeviceRepository implements DeviceRepository {
    @Inject
    AmazonDynamoDB dynamoDB;

    @ConfigProperty(name="io.mirko.alexa.home.raspberry.devices_table")
    String devicesTable;

    @ConfigProperty(name="io.mirko.alexa.home.raspberry.devices_table.index_by_aws_id")
    String indexName;

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

    @Override
    public boolean isValidDevice(String deviceId, String userId) {
        final Map<String, AttributeValue> row = new HashMap<>();
        row.put("device_id", new AttributeValue(deviceId));
        final GetItemResult result = dynamoDB.getItem(devicesTable, row);
        if (result == null || result.getItem() == null) {
            return false;
        }
        final Map<String, AttributeValue> item = result.getItem();
        return item != null && item.get("device_id").getS().equals(deviceId) && item.get("aws_id").getS().equals(userId);
    }

    @Override
    public boolean existsDevice(String deviceId) {
        final Map<String, AttributeValue> row = new HashMap<>();
        row.put("device_id", new AttributeValue(deviceId));
        final GetItemResult result = dynamoDB.getItem(devicesTable, row);
        if (result == null || result.getItem() == null) {
            return false;
        }
        final Map<String, AttributeValue> item = result.getItem();
        return item != null && item.get("device_id").getS().equals(deviceId);
    }

    @Override
    public boolean deleteDevice(String deviceId, String accountId) {
        final Map<String, AttributeValue> row = new HashMap<>();
        row.put("device_id", new AttributeValue(deviceId));
        row.put("aws_id", new AttributeValue(accountId));

        try {
            dynamoDB.deleteItem(devicesTable, row);
        } catch(ResourceNotFoundException | ConditionalCheckFailedException e) {
            System.out.format("Could not find device %s\n", deviceId);
            return false;
        }
        return true;
    }


    @Override
    public Iterable<Device> getDevices(String accountId) {
        final QueryRequest spec = new QueryRequest()
                .withTableName(devicesTable).withIndexName(indexName)
                .withKeyConditionExpression("#aws_id = :aws_id")
                .withExpressionAttributeNames(Collections.singletonMap("#aws_id", "aws_id"))
                .withExpressionAttributeValues(Collections.singletonMap(":aws_id", new AttributeValue(accountId)));
        final QueryResult items = dynamoDB.query(spec);
        return items.getItems().stream().map(m -> new Device(m.get("device_id").getS())).collect(Collectors.toList());
    }
}
