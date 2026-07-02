package com.goungue.backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    // Clé secrète générée pour signer les tokens (en dev, une chaîne fixe suffit)
    private final SecretKey key = Keys.hmacShaKeyFor(
            "goungue-secret-key-pour-jwt-tres-longue-et-securisee-2026".getBytes()
    );

    private static final long EXPIRATION_MS = 1000 * 60 * 60 * 24; // 24h

    public String genererToken(String email) {
        Date maintenant = new Date();
        Date expiration = new Date(maintenant.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(maintenant)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extraireEmail(String token) {
        return extraireClaims(token).getSubject();
    }

    public boolean estValide(String token) {
        try {
            Claims claims = extraireClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extraireClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}