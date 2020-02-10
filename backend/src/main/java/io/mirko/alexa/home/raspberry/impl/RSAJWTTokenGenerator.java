package io.mirko.alexa.home.raspberry.impl;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.mirko.alexa.home.raspberry.JWTTokenGenerator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;


@ApplicationScoped
@Named
public class RSAJWTTokenGenerator implements JWTTokenGenerator {
    @Inject
    RSAPrivateCrtKey rsaKey;

    @Override
    public String generateToken(String deviceId) {
        final Map headers = Jwts.jwsHeader().setKeyId("io.mirko.raspberry").setAlgorithm("RS256");
        return Jwts.builder().setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setSubject(deviceId)
                .setHeader(headers)
                .setIssuer("raspberry.alexa.mirko.io")
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(SignatureAlgorithm.RS256, rsaKey)
                .compact();
    }
}
