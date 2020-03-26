package io.mirko.alexa.home.raspberry;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import javax.inject.Named;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Named("openid_configuration")
public class OpenIDConfigurationLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("isBase64Encoded", false);
        result.put("statusCode", 200);

        final String strOpenIDConfiguration =
                new String(Utils.getDataFromClassloader("io/mirko/openid_configuration.json"));
        result.put(
                "body",
                strOpenIDConfiguration.replaceAll(
                                "\\$\\{OPENID_URL}",
                                System.getenv("OPENID_ROOT_URL")
                )
        );
        return result;
    }
}
