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
    private static byte[] getDataFromClassloader(String resourceName) {
        try(InputStream is = OpenIDConfigurationLambda.class.getClassLoader().getResourceAsStream(resourceName);) {
            final byte[] buffer = new byte[32768];

            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            for (int i = is.read(buffer); i >= 0; i = is.read(buffer)) {
                os.write(buffer, 0, i);
            }
            return os.toByteArray();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("isBase64Encoded", false);
        result.put("statusCode", 200);

        final String strOpenIDConfiguration =
                new String(getDataFromClassloader("io/mirko/openid_configuration.json"));
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
