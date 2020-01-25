package io.mirko.lambda.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.util.Optional;

import static com.amazon.ask.request.Predicates.intentName;

@ApplicationScoped
@Named
public class FallbackIntentHandler implements RequestHandler {

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(intentName("AMAZON.FallbackIntent"));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        String speechText = "Non ho capito, nel dubbio dio bestia";
        return input.getResponseBuilder()
                .withSpeech(speechText)
                .withSimpleCard("Swear", speechText)
                .withReprompt(speechText)
                .build();
    }

}