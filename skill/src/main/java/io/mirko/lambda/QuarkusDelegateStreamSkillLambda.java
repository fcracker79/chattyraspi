package io.mirko.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mirko.impl.AWSProfileService;
import io.mirko.repository.*;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Named("lambda_entry_point")
public class QuarkusDelegateStreamSkillLambda implements RequestHandler<Map, Map> {
    private static final int NUM_SECONDS_TO_WAIT_EXECUTION = 15;
    private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();

    @Inject
    DevicesFetcher devicesFetcher;

    @Inject
    @RestClient
    AWSProfileService profileService;

    @Inject
    CommandSubmitter commandSubmitter;

    @Inject
    CommandStatusFetcher commandStatusFetcher;

    @Override
    public Map handleRequest(Map request, Context context) {
        System.out.format("************ Handling request %s\n", request);
        Optional<String> namespace = getFromMap(request, "directive.header.namespace");
        Optional<String> header = getFromMap(request, "directive.header.name");
        if ("Alexa.Discovery".equals(namespace.orElse(null)) && "Discover".equals(header.orElse(null))) {
            return discovery(request, context);
        } else if ("Alexa.PowerController".equals(namespace.orElse(null))) {
            final String deviceId = (String)
                    getFromMap(request, "directive.endpoint.endpointId").orElseThrow(RuntimeException::new);
            if ("TurnOn".equals(header.orElse(null))) {
                return turnOn(request, context, deviceId);
            } else if ("TurnOff".equals(header.orElse(null))) {
                return turnOff(request, context, deviceId);
            } else {
                throw new IllegalStateException(String.format("Unexpected header name %s", header));
            }
        } else {
            throw new IllegalStateException(String.format("Unexpected request %s", request));
        }
    }

    private Map<String, Object> turnOff(Map<String, Object> request, Context context, String deviceId) {
        final String commandId = commandSubmitter.submitCommand(deviceId, CommandType.TURN_ON);
        return waitForCommandExecuted(request, commandId, "ON", deviceId);
    }

    private Map<String, Object> turnOn(Map<String, Object> request, Context context, String deviceId) {
        final String commandId = commandSubmitter.submitCommand(deviceId, CommandType.TURN_OFF);
        return waitForCommandExecuted(request, commandId, "OFF", deviceId);
    }

    private Map<String, Object> waitForCommandExecuted(
            Map<String, Object> request,
            String commandId, String powerState, String endpointId) {
        for (int i = 0; i < NUM_SECONDS_TO_WAIT_EXECUTION; i++) {
            if (commandStatusFetcher.getCommandStatus(commandId) == CommandStatus.NOT_FOUND) {
                final String token = (String)
                        getFromMap(request, "directive.endpoint.scope.token").orElseThrow(RuntimeException::new);
                final String correlationToken = (String)
                        getFromMap(request, "directive.header.correlationToken").orElseThrow(RuntimeException::new);
                AlexaResponse ar = new AlexaResponse("Alexa", "Response", endpointId, token, correlationToken);
                ar.AddContextProperty("Alexa.PowerController", "powerState", powerState, 200);
                return toMap(ar);
            }
            try {
                Thread.sleep(1000l);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return toMap(new AlexaResponse("Alexa", "ErrorResponse"));
    }

    private Map<String, Object> discovery(Map<String, Object> request, Context context) {
        final String accountId = profileService.getProfile(
                (String) getFromMap(request, "directive.payload.scope.token").orElseThrow(RuntimeException::new)
        ).user_id;

        System.out.println("Found Alexa.Discovery Namespace");
        AlexaResponse ar = new AlexaResponse("Alexa.Discovery", "Discover.Response");
        String capabilityAlexa = ar.CreatePayloadEndpointCapability(
                "AlexaInterface", "Alexa", "3", null
        );
        String capabilityAlexaPowerController = ar.CreatePayloadEndpointCapability(
                "AlexaInterface", "Alexa.PowerController", "3",
                "{\"supported\": [ { \"name\": \"powerState\" } ] }");
        String capabilities = "[" + capabilityAlexa + ", " + capabilityAlexaPowerController + "]";
        int i = 1;
        for (Device d : devicesFetcher.getDevices(accountId)) {
            ar.AddPayloadEndpoint(String.format("Raspberry %s", i), d.deviceId, capabilities);
            i += 1;
        }
        return toMap(ar);
    }

    private Map<String, Object> toMap(AlexaResponse ar) {
        final String strResponse = ar.toString();
        try {
            return JSON_OBJECT_MAPPER.readValue(strResponse, Map.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> Optional<T> getFromMap(Map m, String path) {
        Object lastValue = m;
        for (String field : path.split("\\.")) {
            if (lastValue == null) {
                return Optional.empty();
            }
            lastValue = ((Map) lastValue).get(field);
        }
        if (lastValue == null) {
            return Optional.empty();
        }
        return Optional.of((T) lastValue);
    }
}
