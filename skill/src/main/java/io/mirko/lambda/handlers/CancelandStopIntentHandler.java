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
public class CancelandStopIntentHandler implements RequestHandler {
    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(intentName("AMAZON.StopIntent").or(intentName("AMAZON.CancelIntent")));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        String speechText = "E' cosa buona e giusta";
        return input.getResponseBuilder()
                .withSpeech(speechText)
                .withSimpleCard("Swear", speechText)
                .build();
    }
}