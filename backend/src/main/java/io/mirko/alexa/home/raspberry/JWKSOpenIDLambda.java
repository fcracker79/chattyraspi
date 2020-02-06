package io.mirko.alexa.home.raspberry;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.inject.Inject;
import javax.inject.Named;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Named("jwks")
public class JWKSOpenIDLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    @Inject
    RSAPrivateCrtKey rsaKey;

    private String resultBody;

    private static String encode(BigInteger b, int length) {
        final byte[] from = b.toByteArray();
        final byte[] to = new byte[length];
        System.arraycopy(from, 0, to, 0, from.length);
        return new String(Base64.getEncoder().encode(to)).replaceAll("=$", "");
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        if (resultBody == null) {

            BigInteger n = rsaKey.getModulus();
            BigInteger e = rsaKey.getPublicExponent();
            final Map<String, Object> resultBodyMap = new HashMap<>();
            Map<String, Object> keyMap = new HashMap<>();
            keyMap.put("e", encode(e, 3));
            keyMap.put("n", encode(n, 257));
            keyMap.put("usr", "sig");
            keyMap.put("kty", "RSA");
            keyMap.put("kid", "io.mirko.raspberry");
            keyMap.put("alg", "RS256");
            keyMap = Collections.unmodifiableMap(keyMap);
            resultBodyMap.put("keys", Collections.singletonList(keyMap));
            try {
                resultBody = JSON_MAPPER.writeValueAsString(resultBodyMap);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        }
        HashMap<String, Object> result = new HashMap<>();
        result.put("isBase64Encoded", false);
        result.put("statusCode", 200);
        result.put("body", resultBody);
        return result;
    }
}
