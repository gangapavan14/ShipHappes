package com.shiphappens.logistics.security;
import static org.junit.jupiter.api.Assertions.*; import org.junit.jupiter.api.Test;
class JwtServiceTest { @Test void createsAndParsesSignedClaims(){JwtService jwt=new JwtService("this-is-a-local-test-secret-that-is-at-least-32-characters-long",60);var claims=jwt.parse(jwt.create("ops@demo.invalid","OPERATIONS",12L));assertEquals("ops@demo.invalid",claims.getSubject());assertEquals("OPERATIONS",claims.get("role",String.class));assertEquals(12,claims.get("uid",Integer.class));} }
