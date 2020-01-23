package io.mirko.alexa.home.raspberry.aws;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

public class DynamoDBFactory {
    @Produces
    @ApplicationScoped
    @Named
    public AmazonDynamoDB createDynamoDB() {
        return AmazonDynamoDBClientBuilder.defaultClient();
    }
}
