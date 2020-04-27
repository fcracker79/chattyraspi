package io.mirko.alexa.home.raspberry.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import io.mirko.alexa.home.raspberry.Device;
import io.mirko.alexa.home.raspberry.DeviceRepository;
import io.mirko.alexa.home.raspberry.UserRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;
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

    @Inject
    UserRepository userRepository;

    @Override
    public UUID registerDevice(String deviceName, String accessToken) {
        final Map<String, AttributeValue> row = new HashMap<>();
        final UUID deviceId = UUID.randomUUID();
        row.put("device_id", new AttributeValue(deviceId.toString()));
        row.put("device_name", new AttributeValue(deviceName));
        // e.g. {"user_id":"amzn1.account.AE...RQ","name":"John Burns","email":"john@burns.com"}'
        AWSProfile profile = awsProfileService.getProfile(accessToken);
        userRepository.saveUser(profile);
        row.put("aws_id", new AttributeValue(profile.user_id));
        dynamoDB.putItem(devicesTable, row);
        return deviceId;
    }

    @Override
    public boolean deleteDevice(UUID deviceId, String accountId) {
        final Map<String, AttributeValue> row = new HashMap<>();
        row.put("device_id", new AttributeValue(deviceId.toString()));
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
    public List<Device> getDevices(String accountId) {
        final QueryRequest spec = new QueryRequest()
                .withTableName(devicesTable).withIndexName(indexName)
                .withKeyConditionExpression("#aws_id = :aws_id")
                .withExpressionAttributeNames(Collections.singletonMap("#aws_id", "aws_id"))
                .withExpressionAttributeValues(Collections.singletonMap(":aws_id", new AttributeValue(accountId)));
        final QueryResult items = dynamoDB.query(spec);
        return items.getItems().stream().map(
                m -> new Device(UUID.fromString(m.get("device_id").getS()), m.get("device_name").getS()))
                .collect(Collectors.toList());
    }
}
