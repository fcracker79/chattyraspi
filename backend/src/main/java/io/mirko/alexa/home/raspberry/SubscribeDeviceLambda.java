package io.mirko.alexa.home.raspberry;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mirko.alexa.home.raspberry.exceptions.UnauthorizedException;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Named("subscribe")
public class SubscribeDeviceLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    JWTTokenGenerator tokenGenerator;

    @Inject
    DeviceRepository deviceRepository;

    @Inject
    DeviceCreationPolicy deviceCreationPolicy;

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        System.out.format("Subscription, handling request input %s, context %s\n", input, context);
        HashMap<String, Object> result = new HashMap<>();
        result.put("isBase64Encoded", false);
        result.put("statusCode", 200);

        final Map<String, Object> body;

        try {
            body = MAPPER.readValue((String) input.get("body"), Map.class);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        String accessToken = (String) body.get("access_token");;
        String deviceName = (String) body.get("device_name");
        System.out.format("Registering device %s, access token %s", deviceName, accessToken);
        final HashMap<String, Object> responseBody = new HashMap<>();
        try {
            if (deviceCreationPolicy.canCreateNewDevice(accessToken)) {
                final UUID deviceId =deviceRepository.registerDevice(deviceName, accessToken);
                responseBody.put("result", "success");
                responseBody.put("data", tokenGenerator.generateToken(deviceId));
            } else {
                result.put("statusCode", 403);
                responseBody.put("result", "failure");
                responseBody.put("data", "TOO_MANY_DEVICES");
            }
        } catch(UnauthorizedException e) {
            System.err.println("Error attempting to use accessToken. We will hide the issue for security reasons");
            throw e;
        }

        try {
            result.put("body", MAPPER.writeValueAsString(responseBody));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
