package io.mirko.impl.repository;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@SuppressWarnings("unused")
public class RSAPrivateKeyFactory {
    private final Logger logger = LoggerFactory.getLogger(RSAPrivateKeyFactory.class);

    private final AWSSimpleSystemsManagement client =
            AWSSimpleSystemsManagementClientBuilder.defaultClient();

    @Produces
    @ApplicationScoped
    @Named
    public RSAPrivateCrtKey create() {
        logger.debug("Creating RSA Private key");
        String sRSAKey = client.getParameter(
                new GetParameterRequest().withName(System.getenv("SSM_KEY_NAME")).withWithDecryption(true)
        ).getParameter().getValue().replaceAll("\n", "");
        logger.debug("Creating RSA Private key, SSM key extraction complete");
        byte[] bRSAKey = Base64.getDecoder().decode(sRSAKey.getBytes());
        logger.debug("My key is {} bytes long", bRSAKey.length);
        return createFromBytes(bRSAKey);
    }

    private RSAPrivateCrtKey createFromBytes(byte[] bRSAKey) {
        final RSAPrivateCrtKey privKey;
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(bRSAKey);
            privKey = (RSAPrivateCrtKey) kf.generatePrivate(keySpecPKCS8);
            return privKey;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
