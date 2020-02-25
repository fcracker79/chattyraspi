package io.mirko.impl.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import io.mirko.repository.Device;
import io.mirko.repository.DevicesFetcher;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.stream.Collectors;


@ApplicationScoped
@Named
public class DynamoDBDevicesFetcher implements DevicesFetcher {
    @Inject
    AmazonDynamoDB dynamoDB;

    @ConfigProperty(name="io.mirko.alexa.home.raspberry.devices_table")
    String devicesTable;

    @ConfigProperty(name="io.mirko.alexa.home.raspberry.devices_table.index_by_aws_id")
    String indexName;

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
