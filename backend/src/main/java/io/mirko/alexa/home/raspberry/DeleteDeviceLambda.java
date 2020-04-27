package io.mirko.alexa.home.raspberry;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mirko.alexa.home.raspberry.impl.AWSProfile;
import io.mirko.alexa.home.raspberry.impl.AWSProfileService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Named("deleteDevice")
public class DeleteDeviceLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    DeviceRepository deviceRepository;

    @Inject
    @RestClient
    AWSProfileService profileService;

    @Inject
    UserRepository userRepository;

    @ConfigProperty(name="io.mirko.alexa.home.raspberry.devices_table.index_by_aws_id")
    String indexName;

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        System.out.format("Delete device, handling request input %s, context %s\n", input, context);
        HashMap<String, Object> result = new HashMap<>();
        result.put("isBase64Encoded", false);
        result.put("statusCode", 200);

        final String accessToken = ((Map<String, String>) input.get("headers")).get("access-token");
        if (accessToken != null) {
            final String accountId;

            try {
                AWSProfile profile = profileService.getProfile(accessToken);
                accountId = profile.user_id;
                userRepository.saveUser(profile);
            } catch (WebApplicationException e) {
                System.err.println("Could not access account id from token");
                e.printStackTrace();
                result.put("statusCode", 401);
                return result;
            }
            HashMap<String, Object> responseBody = new HashMap<>();
            responseBody.put("result", "success");
            try {
                result.put("body", MAPPER.writeValueAsString(responseBody));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            final String deviceId = (String) ((Map<String, Object>) input.get("pathParameters")).get("deviceId");
            if (!deviceRepository.deleteDevice(UUID.fromString(deviceId), accountId)) {
                result.put("statusCode", 404);
            }
        } else {
                result.put("statusCode", 401);
        }

        return result;
    }
}
