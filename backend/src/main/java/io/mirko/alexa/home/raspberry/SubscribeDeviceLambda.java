package io.mirko.alexa.home.raspberry;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mirko.alexa.home.raspberry.exceptions.UnauthorizedException;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

@Named("subscribe")
public class SubscribeDeviceLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    DeviceRepository deviceRepository;

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        System.out.format("Subscription, handling request input %s, context %s\n", input, context);
        HashMap<String, Object> result = new HashMap<>();
        result.put("isBase64Encoded", false);
        result.put("statusCode", 200);

        String accessToken = (String) ((Map<String, Object>) input.get("body")).get("access_token");
        String deviceId = (String) ((Map<String, Object>) input.get("body")).get("device_id");
        System.out.format("Registering device %s, access token %s", deviceId, accessToken);
        try {
            deviceRepository.registerDevice(deviceId, accessToken);
        } catch(UnauthorizedException e) {
            System.err.println("Error attempting to use accessToken. We will hide the issue for security reasons");
            e.printStackTrace();
        }
        HashMap<String, Object> body = new HashMap<>();
        body.put("result", "success");
        try {
            result.put("body", MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
