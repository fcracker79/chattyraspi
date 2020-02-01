package io.mirko.impl;


import io.mirko.lambda.QuarkusDelegateStreamSkillLambda;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SmokeTest {
    @Test
    @Disabled
    public void smokeTest() {
        new QuarkusDelegateStreamSkillLambda();
        // Assertions.assertEquals(
        //         17,
        //         CDI.current().getBeanManager().getBeans(RequestHandler.class).size()
        // );
        // Assertions.assertEquals(
        //         1,
        //         CDI.current().getBeanManager().getBeans(CancelandStopIntentHandler.class).size()
        // );
    }
}
