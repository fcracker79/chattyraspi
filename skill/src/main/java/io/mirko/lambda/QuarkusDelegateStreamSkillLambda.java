package io.mirko.lambda;

import com.amazon.ask.SkillStreamHandler;
import com.amazon.ask.Skills;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.request.interceptor.GenericRequestInterceptor;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Stream;

@Named("lambda_entry_point")
public class QuarkusDelegateStreamSkillLambda implements RequestHandler<Map, Map> {
    private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();

    @Override
    public Map handleRequest(Map request, Context context) {
        System.out.format("************ Handling request %s\n", request);
        Optional<String> namespace = getFromMap(request, "directive.header.namespace");
        Optional<String> header = getFromMap(request, "directive.header.name");
        if ("Alexa.Discovery".equals(namespace.orElse(null)) && "Discover".equals(header.orElse(null))) {
            return discovery(request, context);
        }
        throw new IllegalStateException(String.format("Unexpected %s", request));
    }

    private Map<String, Object> discovery(Map<String, Object> request, Context context) {
        System.out.println("Found Alexa.Discovery Namespace");
        AlexaResponse ar = new AlexaResponse("Alexa.Discovery", "Discover.Response");
        String capabilityAlexa = ar.CreatePayloadEndpointCapability("AlexaInterface", "Alexa", "3", null);
        String capabilityAlexaPowerController = ar.CreatePayloadEndpointCapability("AlexaInterface", "Alexa.PowerController", "3", "{\"supported\": [ { \"name\": \"powerState\" } ] }");
        String capabilities = "[" + capabilityAlexa + ", " + capabilityAlexaPowerController + "]";
        // TODO add real devices
        ar.AddPayloadEndpoint("Sample Switch", "sample-switch-01", capabilities);
        final String strResponse = ar.toString();
        try {
            return JSON_OBJECT_MAPPER.readValue(strResponse, Map.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> Stream<T> getBeans(Type t) {
        final BeanManager bm = CDI.current().getBeanManager();
        //noinspection unchecked
        return (Stream<T>) bm.getBeans(t).stream()
                .map(b -> bm.getReference(b, t, bm.createCreationalContext(b)));
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
