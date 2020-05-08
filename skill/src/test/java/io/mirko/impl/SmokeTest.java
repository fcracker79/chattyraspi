package io.mirko.impl;


import io.mirko.lambda.QuarkusDelegateStreamSkillLambda;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@QuarkusTest
public class SmokeTest {
    @Test
    public void smokeTest() {
        new QuarkusDelegateStreamSkillLambda();
        Logger logger = LoggerFactory.getLogger("io.mirko.test1");
        logger.info("io.mirko info");
        logger.debug("io.mirko debug");

        logger = LoggerFactory.getLogger("io.quarkus.test1");
        logger.info("io.quarkus info");
        logger.debug("io.quarkus debug");
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
