package io.mirko.alexa.home.raspberry;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.mirko.alexa.home.raspberry.impl.AWSProfile;
import io.mirko.alexa.home.raspberry.impl.AWSProfileService;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.StreamSupport;

@Named("devices_configuration")
public class GetConfigurationLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private String configurationFormat;
    @Inject
    DeviceRepository deviceRepository;

    @Inject
    @RestClient
    AWSProfileService profileService;

    @Inject
    UserRepository userRepository;

    @Inject
    JWTTokenGenerator tokenGenerator;

    public GetConfigurationLambda() {
        Velocity.setProperties(Utils.getPropertiesFromClassloader("io/mirko/velocity.properties"));
    }

    private String getConfigurationFormat() {
        // Multithreading? Synchronization? Volatile variable? Not an issue, as we have a separate
        // instance for each request
        if (configurationFormat == null) {
            configurationFormat = new String(Utils.getDataFromClassloader("io/mirko/devices_configuration_template.yaml"));
        }
        return configurationFormat;
    }
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        System.out.format("List devices, handling request input %s, context %s\n", input, context);
        HashMap<String, Object> result = new HashMap<>();
        result.put("isBase64Encoded", false);
        result.put("statusCode", 200);


        final String accessToken = ((Map<String, String>) input.get("headers")).get("access-token");
        HashMap<String, Object> responseBody = new HashMap<>();

        if (accessToken != null) {
            AWSProfile profile = profileService.getProfile(accessToken);
            final String accountId = profile.user_id;
            userRepository.saveUser(profile);
            responseBody.put("result", "success");
            final Iterator<Map<String, Object>> devices =
                    StreamSupport
                        .stream(deviceRepository.getDevices(accountId).spliterator(), false)
                        .map(device -> {
                            final Map<String, Object> deviceMap = new HashMap<>();
                            deviceMap.put("device_id", device.deviceId.toString());
                            deviceMap.put("device_name", device.deviceName);
                            deviceMap.put("openid_token", tokenGenerator.generateToken(device.deviceId));
                            return deviceMap;
                        })
                        .iterator();
            final VelocityContext velocityContext = new VelocityContext();
            velocityContext.put("devices", devices);
            velocityContext.put("account_id", accountId);
            final Template template = Velocity.getTemplate("io/mirko/devices_configuration_template.yaml");
            final StringWriter sw = new StringWriter();
            template.merge(velocityContext, sw );
            result.put("body", sw.toString());
            result.put("headers", Collections.singletonMap("Content-Type", "application/x-yaml"));
        } else {
            responseBody.put("result", "failure");
        }
        return result;
    }
}
