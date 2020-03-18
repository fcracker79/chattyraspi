package io.mirko.alexa.home.raspberry;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mirko.alexa.home.raspberry.exceptions.UnauthorizedException;
import io.mirko.alexa.home.raspberry.impl.AWSProfileService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Named("listDevices")
public class ListDevicesLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    DeviceRepository deviceRepository;

    @Inject
    @RestClient
    AWSProfileService profileService;

    @ConfigProperty(name="io.mirko.alexa.home.raspberry.devices_table.index_by_aws_id")
    String indexName;

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        System.out.format("List devices, handling request input %s, context %s\n", input, context);
        HashMap<String, Object> result = new HashMap<>();
        result.put("isBase64Encoded", false);
        result.put("statusCode", 200);


        final String accessToken = ((Map<String, String>) input.get("headers")).get("access-token");
        HashMap<String, Object> responseBody = new HashMap<>();

        if (accessToken != null) {
            responseBody.put("result", "success");
            final Iterable<Device> devices = deviceRepository.getDevices(profileService.getProfile(accessToken).user_id);
            responseBody.put(
                    "devices",
                    StreamSupport.stream(devices.spliterator(), false).map(
                            device -> {
                                final Map<String, Object> deviceMap = new HashMap<>();
                                deviceMap.put("deviceId", device.deviceId);
                                return deviceMap;
                            }
                    ).collect(Collectors.toList())
            );
        } else {
            responseBody.put("result", "failure");
        }
        try {
            result.put("body", MAPPER.writeValueAsString(responseBody));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
