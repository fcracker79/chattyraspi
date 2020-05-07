package io.mirko.impl.repository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;


@ApplicationScoped
@Named
public class RSAJWTTokenGenerator {
    @Inject
    RSAPrivateCrtKey rsaKey;

    private String cachedJwtToken = null;
    public String generateToken() {
        if (cachedJwtToken != null) {
            return cachedJwtToken;
        }
        System.out.println("Generating JWT Token");
        final Map headers = Jwts.jwsHeader().setKeyId("io.mirko.raspberry").setAlgorithm("RS256");
        cachedJwtToken = Jwts.builder().setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setSubject("admin")
                .setHeader(headers)
                .setIssuer("raspberry.alexa.mirko.io")
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(SignatureAlgorithm.RS256, rsaKey)
                .compact();
        System.out.println("JWT Token generation successful");
        return cachedJwtToken;
    }
}
