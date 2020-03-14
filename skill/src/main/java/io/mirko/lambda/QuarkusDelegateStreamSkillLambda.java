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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Named("lambda_entry_point")
public class QuarkusDelegateStreamSkillLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {
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

    @Inject
    CommandResponseFetcher commandResponseFetcher;

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> request, Context context) {
        System.out.format("************ Handling request %s\n", request);

        if (request.containsKey("directive")) {
            return manageDirective(request, context);
        }
        throw new RuntimeException(String.format("Unexpected payload %s", request));
    }

    private Map<String, Object> manageDirective(Map<String, Object> request, Context context) {
        final RequestFilter filter = RequestFilter.withRequest(request);
        if (filter.withHeaderNamespace("Alexa.Discovery").withHeaderName("Discover").filter()) {
            return discovery(request);
        }

        if (filter.withHeaderNamespace("Alexa.PowerController").filter()) {
            if (filter.withHeaderName("TurnOn").filter()) {
                return turnOn(request, getDeviceId(request));
            } else if (filter.withHeaderName("TurnOff").filter()) {
                return turnOff(request, getDeviceId(request));
            } else {
                throw new IllegalStateException(String.format("Unexpected header name for request %s", request));
            }
        }

        if (filter.withHeaderNamespace("Alexa").withHeaderName("ReportState").filter()) {
            return stateReport(request, getDeviceId(request));
        }

        throw new IllegalStateException(String.format("Unexpected request %s", request));
    }

    private String getDeviceId(Map<String, Object> request) {
        return (String) getFromMap(request, "directive.endpoint.endpointId").orElseThrow(RuntimeException::new);
    }

    private Map<String, Object> turnOff(Map<String, Object> request, String deviceId) {
        final String commandId = commandSubmitter.submitCommand(deviceId, CommandType.TURN_OFF);
        return waitForPowerStateControlCompleted(request, commandId, "OFF", deviceId, "Response");
    }

    private Map<String, Object> turnOn(Map<String, Object> request, String deviceId) {
        final String commandId = commandSubmitter.submitCommand(deviceId, CommandType.TURN_ON);
        return waitForPowerStateControlCompleted(request, commandId, "ON", deviceId, "Response");
    }

    private Map<String, Object> stateReport(Map<String, Object> request, String deviceId) {
        final String commandId = commandSubmitter.submitCommand(deviceId, CommandType.POWER_STATUS);
        return waitForPowerStateControlCompleted(request, commandId, null, deviceId, "StateReport");
    }

    private Map<String, Object> waitForPowerStateControlCompleted(
            Map<String, Object> request,
            String commandId, String powerState, String endpointId,
            String responseName) {
        System.out.format("Waiting for command to be executed, device %s, responseName %s...", endpointId, responseName);
        for (int i = 0; i < NUM_SECONDS_TO_WAIT_EXECUTION; i++) {
            final CommandStatus currentStatus = commandStatusFetcher.getCommandStatus(commandId);
            System.out.format("Waiting for command to be executed, status %s\n", currentStatus);
            if (currentStatus == CommandStatus.NOT_FOUND) {
                final String token = (String)
                        getFromMap(request, "directive.endpoint.scope.token").orElseThrow(RuntimeException::new);
                final String correlationToken = (String)
                        getFromMap(request, "directive.header.correlationToken").orElseThrow(RuntimeException::new);
                AlexaResponse ar = new AlexaResponse("Alexa", responseName, endpointId, token, correlationToken);
                if (powerState == null) {
                    powerState = "ON".equals(commandResponseFetcher.getCommandResponse(commandId).orElse(null)) ?
                            "ON" : "OFF";
                }
                ar.AddContextProperty("Alexa.PowerController", "powerState", powerState, 200);
                return toMap(ar);
            }
            System.out.println("Retrying in 1 second's time");
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return toMap(new AlexaResponse("Alexa", "ErrorResponse"));
    }

    private Map<String, Object> discovery(Map<String, Object> request) {
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
                "{\"supported\": [ { \"name\": \"powerState\" } ], \"proactivelyReported\": true,\n" +
                        "                \"retrievable\": true }");
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
            System.out.format("Response: %s\n", strResponse);
            //noinspection unchecked
            return JSON_OBJECT_MAPPER.readValue(strResponse, Map.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> Optional<T> getFromMap(Map<String, Object> m, String path) {
        Object lastValue = m;
        for (String field : path.split("\\.")) {
            if (lastValue == null) {
                return Optional.empty();
            }
            //noinspection unchecked
            lastValue = ((Map<String, Object>) lastValue).get(field);
        }
        if (lastValue == null) {
            return Optional.empty();
        }
        //noinspection unchecked
        return Optional.of((T) lastValue);
    }

    public static class RequestFilter {
        private final Map<String, Object> request;
        private Map<String, Object> filters = new HashMap<>();
        private Map<String, Object> cachedRequestValues = new HashMap<>();

        private RequestFilter(Map<String, Object> request) {
            this.request = request;
        }

        private RequestFilter(RequestFilter other) {
            this(other.request);
            this.filters.putAll(other.filters);
            this.cachedRequestValues.putAll(other.cachedRequestValues);
        }

        public static RequestFilter withRequest(Map<String, Object> request) {
            return new RequestFilter(request);
        }

        public RequestFilter withHeaderNamespace(String expectedValue) {
            return withFilter("directive.header.namespace", expectedValue);
        }

        public RequestFilter withHeaderName(String expectedValue) {
            return withFilter("directive.header.name", expectedValue);
        }

        public RequestFilter withFilter(String path, Object expectedValue) {
            final RequestFilter r = new RequestFilter(this);
            r.filters.put(path, expectedValue);
            return r;
        }

        private <T> Optional<T> getRequestValue(String field) {
            final Optional<T> t;

            if (cachedRequestValues.containsKey(field)) {
                //noinspection unchecked
                t = (Optional<T>) cachedRequestValues.get(field);
            } else {
                //noinspection unchecked
                t = (Optional<T>) getFromMap(request, field);
                cachedRequestValues.put(field, t);
            }
            return t;
        }

        public boolean filter() {
            for (Map.Entry<String, Object> e: filters.entrySet()) {
                if (!e.getValue().equals(getRequestValue(e.getKey()).orElse(null))) {
                    return false;
                }
            }
            return true;
        }
    }
}
