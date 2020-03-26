package io.mirko.alexa.home.raspberry;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class Main {
    private static final String RSA_KEY_FILENAME = "/home/mypc/dev/terraform/home_terraform/modules/raspberry_lambda/openid_private_key.pkcs8.b64";
    private static byte[] getRSAKey() {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[32768];
        try(FileInputStream is = new FileInputStream(RSA_KEY_FILENAME)) {
            for (int i = is.read(buffer); i >= 0; i = is.read(buffer)) {
                if (buffer[i - 1] == '\n') {
                    i -= 1;
                }
                os.write(buffer, 0, i);
            }
            return Base64.getDecoder().decode(os.toByteArray());
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static RSAPrivateCrtKey createFromBytes(byte[] bRSAKey) {
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

    public static void main(String ... args) {
        Map headers = Jwts.jwsHeader().setKeyId("io.mirko.raspberry").setAlgorithm("RS256");
        System.out.println(
            Jwts.builder().setId(UUID.randomUUID().toString())
                    .setIssuedAt(new Date())
                    .setHeader(headers)
                    .setSubject("admin")
                    .setIssuer("raspberry.alexa.mirko.io")
                    .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                    .signWith(SignatureAlgorithm.RS256, createFromBytes(getRSAKey()))
                    .compact()
        );
    }
}
