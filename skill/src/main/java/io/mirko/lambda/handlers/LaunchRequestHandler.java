package io.mirko.lambda.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.Response;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.requestType;

@ApplicationScoped
@Named
public class LaunchRequestHandler implements RequestHandler {

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(requestType(LaunchRequest.class));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        System.out.format("LaunchRequestHandler, request %s\n", input.getRequest().getClass());
        String speechText = "Devo ancora essere implementato";
        return input.getResponseBuilder()
                .withSpeech(speechText)
                .withSimpleCard("Swear", speechText)
                .withReprompt(speechText)
                .build();
    }

}