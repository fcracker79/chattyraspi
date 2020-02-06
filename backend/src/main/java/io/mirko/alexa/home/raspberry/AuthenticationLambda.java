package io.mirko.alexa.home.raspberry;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mirko.alexa.home.raspberry.impl.AWSProfileService;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


@Named("auth")
public class AuthenticationLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @Inject
    JWTTokenGenerator tokenGenerator;

    @Inject
    DeviceRepository deviceRepository;

    @Inject
    @RestClient
    AWSProfileService profileService;

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        final Map<String, Object> body;
        HashMap<String, Object> result = new HashMap<>();
        result.put("isBase64Encoded", false);

        try {
            //noinspection unchecked
            body = JSON_MAPPER.readValue((String) input.get("body"), Map.class);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        final String deviceId = (String) body.get("device_id");
        final String accessToken = (String) body.get("access_token");
        final String userId = profileService.getProfile(accessToken).user_id;
        final int statusCode;
        if (!deviceRepository.existsDevice(deviceId)) {
            statusCode = 200;
            deviceRepository.registerDevice(deviceId, accessToken);
        } else if (deviceRepository.isValidDevice(deviceId, userId)) {
            statusCode = 200;
        } else {
            statusCode = 401;
        }
        result.put("statusCode", statusCode);
        if (statusCode == 200) {
            try {
                result.put(
                        "body",
                        JSON_MAPPER.writeValueAsString(
                                Collections.singletonMap(
                                        "token",
                                        tokenGenerator.generateToken(deviceId)))
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }
}
