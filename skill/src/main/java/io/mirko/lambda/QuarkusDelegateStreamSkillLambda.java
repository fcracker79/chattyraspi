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
import java.util.Map;
import java.util.stream.Stream;

@Named("lambda_entry_point")
public class QuarkusDelegateStreamSkillLambda implements RequestHandler<Map, Map> {
    private static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();

    private final SkillStreamHandler delegate;
    public QuarkusDelegateStreamSkillLambda() {
        delegate = new SkillStreamHandler(
                Skills.standard()
                        .addRequestHandlers(getBeans(com.amazon.ask.dispatcher.request.handler.RequestHandler.class).toArray(com.amazon.ask.dispatcher.request.handler.RequestHandler[]::new))
                        .addRequestInterceptor(new GenericRequestInterceptor<HandlerInput>() {
                            @Override
                            public void process(HandlerInput handlerInput) {
                                System.out.format("Processing %s\n", handlerInput.getRequest());
                            }
                        })
                        .withSkillId("io.mirko.raspberry")
                        .build()
        ) {
        };
    }

    private InputStream streamFromRequest(Map request) {
        try {
            return new ByteArrayInputStream(
                JSON_OBJECT_MAPPER.writerFor(Map.class).writeValueAsBytes(request)
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map handleRequest(Map request, Context context) {
        System.out.format("************ Handling request %s\n", request);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            delegate.handleRequest(streamFromRequest(request), os, context);
            System.out.format("************ Got response %s\n", new String(os.toByteArray()));
            return JSON_OBJECT_MAPPER.readerFor(Map.class).readValue(os.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    private static <T> Stream<T> getBeans(Type t) {
        final BeanManager bm = CDI.current().getBeanManager();
        //noinspection unchecked
        return (Stream<T>) bm.getBeans(t).stream()
                .map(b -> bm.getReference(b, t, bm.createCreationalContext(b)));
    }
}
