package io.mirko.lambda.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.ExceptionEncounteredRequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.interfaces.system.ExceptionEncounteredRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.util.Optional;

@ApplicationScoped
@Named
public class ExceptionHandler implements ExceptionEncounteredRequestHandler {
    @Override
    public boolean canHandle(HandlerInput input, ExceptionEncounteredRequest exceptionEncounteredRequest) {
        return true;
    }

    @Override
    public Optional<Response> handle(HandlerInput input, ExceptionEncounteredRequest exceptionEncounteredRequest) {
        System.err.format("Cause: %s", exceptionEncounteredRequest);
        return Optional.empty();
    }
}
