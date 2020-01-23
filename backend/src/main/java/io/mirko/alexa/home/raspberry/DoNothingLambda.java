package io.mirko.alexa.home.raspberry;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import javax.inject.Named;

@Named("unreachable")
public class DoNothingLambda implements RequestHandler<InputObject, OutputObject> {
    @Override
    public OutputObject handleRequest(InputObject input, Context context) {
        throw new RuntimeException("Unreachable code");
    }
}
