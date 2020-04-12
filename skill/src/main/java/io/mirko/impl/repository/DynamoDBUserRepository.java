package io.mirko.impl.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import io.mirko.impl.AWSProfile;
import io.mirko.repository.UserRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@Named
public class DynamoDBUserRepository implements UserRepository {
    @Inject
    AmazonDynamoDB dynamoDB;

    @ConfigProperty(name="io.mirko.alexa.home.raspberry.users_table")
    String usersTable;

    @Override
    public void saveUser(AWSProfile profile) {
        if (profile.email == null) {
            return;
        }
        System.out.format("saving user %s\n", profile);
        final Map<String, AttributeValue> row = new HashMap<>();
        row.put("user_id", new AttributeValue(profile.user_id));
        if (profile.name != null) {
            row.put("name", new AttributeValue(profile.name));
        }
        row.put("email", new AttributeValue(profile.email));
        dynamoDB.putItem(usersTable, row);
    }
}
