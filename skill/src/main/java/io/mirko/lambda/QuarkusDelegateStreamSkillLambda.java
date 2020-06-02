package io.mirko.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mirko.impl.AWSProfile;
import io.mirko.impl.AWSProfileService;
import io.mirko.repository.*;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Named("lambda_entry_point")
public class QuarkusDelegateStreamSkillLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final int NUM_SECONDS_TO_WAIT_EXECUTION = 15;
    private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();

    private final Logger logger = LoggerFactory.getLogger(QuarkusDelegateStreamSkillLambda.class);

    @Inject
    DevicesFetcher devicesFetcher;

    @Inject
    UserRepository userRepository;

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
        logger.info("************ Handling request {}", request);

        if (request.containsKey("directive")) {
            return manageDirective(request, context);
        }
        throw new RuntimeException(String.format("Unexpected payload %s", request));
    }

    @SuppressWarnings("unused")
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

        if (filter.withHeaderNamespace("Alexa.ThermostatController").withHeaderName("SetTargetTemperature").filter()) {
            return setTargetTemperature(
                    request, getDeviceId(request),
                    ((Number) getFromMap(request, "directive.payload.targetSetpoint.value").get()).doubleValue()
            );
        }

        if (filter.withHeaderNamespace("Alexa.ThermostatController").withHeaderName("AdjustTargetTemperature").filter()) {
            return adjustTargetTemperature(
                    request, getDeviceId(request),
                    ((Number) getFromMap(request, "directive.payload.targetSetpoint.value").get()).doubleValue()
            );
        }

        if (filter.withHeaderNamespace("Alexa.ThermostatController").withHeaderName("SetThermostatMode").filter()) {
            return setThermostatMode(
                    request, getDeviceId(request),
                    ThermostatMode.valueOf(
                            (String) getFromMap(request, "directive.payload.thermostatMode.value").get()
                    )
            );
        }
        throw new IllegalStateException(String.format("Unexpected request %s", request));
    }

    private Map<String, Object> setThermostatMode(Map<String, Object> request, UUID deviceId, ThermostatMode thermostatMode) {
        final String commandId = commandSubmitter.submitCommand(deviceId, CommandType.SET_THERMOSTAT_MODE, thermostatMode.name());
        return waitForPowerStateControlCompleted(request, commandId, null, deviceId, "Response");
    }

    private Map<String, Object> adjustTargetTemperature(Map<String, Object> request, UUID deviceId, double deltaTemperature) {
        final String commandId = commandSubmitter.submitCommand(deviceId, CommandType.ADJUST_TEMPERATURE, deltaTemperature);
        return waitForPowerStateControlCompleted(request, commandId, null, deviceId, "Response");
    }

    private Map<String, Object> setTargetTemperature(Map<String, Object> request, UUID deviceId, double temperature) {
        final String commandId = commandSubmitter.submitCommand(deviceId, CommandType.SET_TEMPERATURE, temperature);
        return waitForPowerStateControlCompleted(request, commandId, null, deviceId, "Response");
    }

    private UUID getDeviceId(Map<String, Object> request) {
        return UUID.fromString((String) getFromMap(request, "directive.endpoint.endpointId").orElseThrow(RuntimeException::new));
    }

    private Map<String, Object> turnOff(Map<String, Object> request, UUID deviceId) {
        final String commandId = commandSubmitter.submitCommand(deviceId, CommandType.TURN_OFF);
        return waitForPowerStateControlCompleted(request, commandId, "OFF", deviceId, "Response");
    }

    private Map<String, Object> turnOn(Map<String, Object> request, UUID deviceId) {
        final String commandId = commandSubmitter.submitCommand(deviceId, CommandType.TURN_ON);
        return waitForPowerStateControlCompleted(request, commandId, "ON", deviceId, "Response");
    }

    private Map<String, Object> stateReport(Map<String, Object> request, UUID deviceId) {
        final String commandId = commandSubmitter.submitCommand(deviceId, CommandType.POWER_STATUS);
        return waitForPowerStateControlCompleted(request, commandId, null, deviceId, "StateReport");
    }

    private Map<String, Object> waitForPowerStateControlCompleted(
            Map<String, Object> request,
            String commandId, String powerState, UUID deviceId,
            String responseName) {
        logger.info("Waiting for command to be executed, device {}, responseName {}...", deviceId, responseName);
        for (int i = 0; i < NUM_SECONDS_TO_WAIT_EXECUTION; i++) {
            final CommandStatus currentStatus = commandStatusFetcher.getCommandStatus(commandId);
            logger.debug("Waiting for command to be executed, status {}", currentStatus);
            if (currentStatus == CommandStatus.NOT_FOUND || currentStatus == CommandStatus.RESPONDED) {
                final String token = (String)
                        getFromMap(request, "directive.endpoint.scope.token").orElseThrow(RuntimeException::new);
                final String correlationToken = (String)
                        getFromMap(request, "directive.header.correlationToken").orElseThrow(RuntimeException::new);
                AlexaResponse ar = new AlexaResponse("Alexa", responseName, deviceId.toString(), token, correlationToken);
                if (powerState == null) {
                    final Optional<List<String>> optionalCommandResponse = commandResponseFetcher.getCommandResponse(commandId);
                    final List<String> commandResponse = optionalCommandResponse.orElse(Collections.emptyList());
                    powerState = commandResponse.contains("ON") ? "ON" : "OFF";
                    addTemperatureStuff(ar, commandResponse);
                }
                ar.AddContextProperty("Alexa.PowerController", "powerState", powerState, 200);
                addHealthProperty(ar);
                return toMap(ar);
            }
            logger.debug("Retrying in 100 msecs' time");
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return toMap(new AlexaResponse("Alexa", "ErrorResponse"));
    }

    private void addTemperatureStuff(AlexaResponse ar, List<String> commandResponse) {
        logger.debug("Adding temperature stuff, commandResponse: {}", commandResponse);
        Optional<String> temperature = commandResponse.stream().filter(d -> d.contains("temperature:")).findFirst();
        Optional<String> thermostatMode = commandResponse.stream().filter(d -> d.contains("thermostatMode:")).findFirst();
        Optional<String> thermostatTargetSetmode = commandResponse.stream().filter(d -> d.contains("thermostatTargetSetpoint:"))
                .findFirst();

        if (temperature.isPresent()) {
            try {
                final double temperatureValue = new BigDecimal(
                        temperature.get().substring("temperature:".length()).trim()
                ).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
                final Map<String, Object> value = new HashMap<>();
                value.put("value", temperatureValue);
                value.put("scale", "CELSIUS");
                ar.AddContextProperty(
                        "Alexa.TemperatureSensor", "temperature",
                        value,
                        300
                );
            } catch(NumberFormatException e) {
                logger.error(String.format("Could not parse temperature %s", temperature.get()), e);
            }
        }

        if (thermostatMode.isPresent()) {
            final String thermostatModeValue = thermostatMode.get().substring("thermostatMode:".length()).trim();
            if (Arrays.asList("HEAT", "COOL", "AUTO").contains(thermostatModeValue)) {
                ar.AddContextProperty(
                        "Alexa.ThermostatController", "thermostatMode",
                        thermostatModeValue,
                        300
                );
            } else {
                logger.error("Thermostat mode not supported: " + thermostatMode.get());
            }
        }

        if (thermostatTargetSetmode.isPresent()) {
            try {
                final double temperatureValue = new BigDecimal(
                        thermostatTargetSetmode.get().substring("thermostatTargetSetpoint:".length()).trim()
                ).setScale(2, RoundingMode.HALF_EVEN).doubleValue();
                final Map<String, Object> value = new HashMap<>();
                value.put("value", temperatureValue);
                value.put("scale", "CELSIUS");
                ar.AddContextProperty(
                        "Alexa.ThermostatController", "targetSetpoint",
                        value,
                        300
                );
            } catch(NumberFormatException e) {
                logger.error(String.format("Could not parse temperature %s", thermostatTargetSetmode.get()), e);
            }
        }
    }

    private void addHealthProperty(AlexaResponse ar) {
        ar.AddContextProperty(
                "Alexa.EndpointHealth",
                "connectivity",
                Collections.singletonMap("value", "OK"),
                300
        );
    }

    private Map<String, Object> discovery(Map<String, Object> request) {
        AWSProfile profile = profileService.getProfile(
                (String) getFromMap(request, "directive.payload.scope.token").orElseThrow(RuntimeException::new)
        );
        userRepository.saveUser(profile);
        final String accountId = profile.user_id;

        logger.debug("Found Alexa.Discovery Namespace");
        AlexaResponse ar = new AlexaResponse("Alexa.Discovery", "Discover.Response");
        String capabilityAlexa = ar.CreatePayloadEndpointCapability(
                "AlexaInterface", "Alexa", "3", null
        );
        String capabilityAlexaPowerController = ar.CreatePayloadEndpointCapability(
                "AlexaInterface", "Alexa.PowerController", "3",
                "{\"supported\": [ { \"name\": \"powerState\" } ], \"proactivelyReported\": true,\n" +
                        "                \"retrievable\": true }");
        String capabilityAlexaHealth = ar.CreatePayloadEndpointCapability(
                "AlexaInterface", "Alexa.EndpointHealth", "3",
                "{\"supported\": [ { \"name\": \"connectivity\" } ], \"proactivelyReported\": true,\n" +
                        "                \"retrievable\": true }"
        );
        String capabilityTemperatureSensor = ar.CreatePayloadEndpointCapability(
                "AlexaInterface", "Alexa.TemperatureSensor", "3",
                "{\"supported\": [ { \"name\": \"temperature\" } ], \"proactivelyReported\": true,\n" +
                        "                \"retrievable\": true }"
        );
        String capabilityThermostat = ar.CreatePayloadEndpointCapability(
                "AlexaInterface", "Alexa.ThermostatController", "3",
                "{\"supported\": [ " +
                        "{ \"name\": \"targetSetpoint\" }," +
                        "{ \"name\": \"lowerSetpoint\" }," +
                        "{ \"name\": \"upperSetpoint\" }," +
                        "{ \"name\": \"thermostatMode\" }," +
                        " ], " +
                        "\"proactivelyReported\": true,\n" +
                        "\"retrievable\": true }",
                "{" +
                        "\"supportedModes\": [\"HEAT\", \"COOL\", \"AUTO\"]," +
                        "\"supportsScheduling\": false" +
                        "}"
        );
        String capabilities = "[" +
                capabilityAlexa +
                ", " + capabilityAlexaPowerController +
                ", " + capabilityAlexaHealth +
                ", " + capabilityThermostat +
                ", " + capabilityTemperatureSensor +
                "]";
        logger.debug("Alexa.Discovery Capabilities: {}", capabilities);
        devicesFetcher.getDevices(accountId).forEach(
                d -> ar.AddPayloadEndpoint(d.deviceName, d.deviceId.toString(), capabilities)
        );
        return toMap(ar);
    }

    private Map<String, Object> toMap(AlexaResponse ar) {
        final String strResponse = ar.toString();
        try {
            logger.info("Response: {}", strResponse);
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
                t = getFromMap(request, field);
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
